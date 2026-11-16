package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.Product
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class StitchByHerRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun userProfile_authenticatedOwner_createsAndUpdatesProfile() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = StitchByHerRepository(firestore, auth)

        val profile = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.ensureUserProfile(
                userId = uid,
                displayName = "Alice Boutique",
                email = ALICE_EMAIL,
                photoUrl = null
            ).getOrThrow()
        }
        assertEquals("Alice Boutique", profile.fullName)
        assertFalse(profile.isBlocked)

        val updateRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.updateUserProfile(
                fullName = "Alice Couture",
                phoneNumber = "9876543210",
                savedAddresses = listOf("42 Rose Lane, Mumbai"),
                defaultAddress = "42 Rose Lane, Mumbai"
            )
        }
        assertTrue(updateRes.isSuccess)
    }

    @Test
    fun wishlist_authenticatedUser_togglesAndIsolates() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = StitchByHerRepository(firestore, auth)
        val sampleProduct = Product(
            id = "prod_test_wishlist",
            name = "Rose Silk Gown",
            price = 4899.0
        )

        val saved = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.toggleWishlist(sampleProduct, currentlySaved = false).getOrThrow()
        }
        assertTrue(saved)

        val items = withTimeout(FLOW_TIMEOUT_MS) {
            aliceRepo.observeWishlist(aliceUid).first { it.any { item -> item.productId == "prod_test_wishlist" } }
        }
        assertTrue(items.any { it.productId == "prod_test_wishlist" })

        // Cross-user read attempt fails with PERMISSION_DENIED
        signInTestUser(BOB_EMAIL)
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("users").document(aliceUid).collection("wishlist").get().await()
            }
            fail("Expected PERMISSION_DENIED for cross-user wishlist read")
        } catch (e: Exception) {
            val firestoreException = e.unwrapFirestoreException()
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException?.code)
        }
    }

    @Test
    fun orders_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = StitchByHerRepository(firestore, auth)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeProducts().first()
            }
            fail("Expected PERMISSION_DENIED for unauthenticated product read")
        } catch (e: Exception) {
            val firestoreException = e.unwrapFirestoreException()
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException?.code)
        }
    }

    @Test
    fun products_nonAdminWrite_failsWithPermissionDenied() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = StitchByHerRepository(firestore, auth)

        val result = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveProduct(
                Product(
                    name = "Unauthorized Dress",
                    categoryId = "cat_dresses",
                    categoryName = "Dresses",
                    price = 1999.0,
                    originalPrice = 2499.0
                )
            )
        }
        assertTrue(result.isFailure)
        val firestoreException = result.exceptionOrNull()?.unwrapFirestoreException()
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException?.code)
    }

    private companion object {
        const val ALICE_EMAIL = "alice_boutique@test.com"
        const val BOB_EMAIL = "bob_boutique@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
