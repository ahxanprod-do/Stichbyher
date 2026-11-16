const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const DATABASE_ID = "ai-studio-android-stitchby-72280786-873a-41b6-a5da-3dbbe7a37309";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const ADMIN_UID = "admin_789";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function getDb(context) {
  return context.firestore();
}

function pastTimestamp() {
  return new Date(Date.now() - 2000);
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function sampleProductPayload(nowTs) {
  return {
    name: "Rose Silk Evening Gown",
    categoryId: "cat_dresses",
    categoryName: "Dresses",
    price: 4999,
    originalPrice: 6499,
    discountPercent: 23,
    description: "Hand-draped blush silk gown.",
    fabricDetails: "100% Mulberry Silk",
    specifications: "Concealed back zip, floor length",
    highlights: ["Handcrafted", "Breathable silk"],
    sizes: ["S", "M", "L"],
    colors: ["Blush Pink", "Ivory"],
    imageUrls: ["preset://dress_collection"],
    stock: 12,
    isFeatured: true,
    isBestSeller: true,
    isNewArrival: true,
    isTrending: false,
    isSpecialCollection: false,
    isPublished: true,
    ratingAvg: 4.9,
    reviewCount: 3,
    createdAt: nowTs,
    updatedAt: nowTs,
  };
}

test("Unauthenticated user: cannot read products or users", async () => {
  const unauthDb = getDb(testEnv.unauthenticatedContext());
  await assertFails(unauthDb.collection("products").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
});

test("User profile: owner can create and update own profile, cannot escalate isBlocked", async () => {
  const aliceDb = getDb(testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }));
  const ts = pastTimestamp();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      fullName: "Alice Couture",
      email: "alice@example.com",
      phoneNumber: "+919876543210",
      savedAddresses: ["22 Rose Avenue, Mumbai"],
      defaultAddress: "22 Rose Avenue, Mumbai",
      isBlocked: false,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Shadow update / privilege escalation rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isBlocked: true,
      updatedAt: pastTimestamp(),
    })
  );

  // Cross-user PII read rejected
  const bobDb = getDb(testEnv.authenticatedContext(BOB_UID, { email: "bob@example.com", email_verified: true }));
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
});

test("Admin spoof test: unverified admin email is rejected", async () => {
  const spoofDb = getDb(testEnv.authenticatedContext("spoof_1", {
    email: "ahxantalib@gmail.com",
    email_verified: false,
  }));
  const ts = pastTimestamp();

  await assertFails(
    spoofDb.collection("categories").doc("cat_dresses").set({
      name: "Dresses",
      description: "Evening wear",
      sortOrder: 1,
      isVisible: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );
});

test("Verified bootstrapped admin: can manage categories, products, announcements, and websiteSettings", async () => {
  const adminDb = getDb(testEnv.authenticatedContext(ADMIN_UID, {
    email: "ahxantalib@gmail.com",
    email_verified: true,
  }));
  const ts = pastTimestamp();

  await assertSucceeds(
    adminDb.collection("categories").doc("cat_dresses").set({
      name: "Dresses",
      description: "Luxury couture dresses",
      imageUrl: "preset://dress_collection",
      sortOrder: 1,
      isVisible: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    adminDb.collection("products").doc("prod_1").set(sampleProductPayload(ts))
  );

  await assertSucceeds(
    adminDb.collection("products").doc("prod_1").update({
      price: 4499,
      updatedAt: pastTimestamp(),
    })
  );

  // Ghost field shadow write fails
  await assertFails(
    adminDb.collection("announcements").doc("ann_1").set({
      title: "Festive Couture",
      message: "15% off custom bridal stitching",
      badgeText: "NEW",
      sortOrder: 0,
      isActive: true,
      ghostField: "malicious",
      createdAt: ts,
      updatedAt: ts,
    })
  );
});

test("Orders: customer can create Pending WhatsApp order and query own orders, cannot read other user orders or skip status", async () => {
  const ts = pastTimestamp();
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = getDb(ctx);
    await db.collection("products").doc("prod_1").set(sampleProductPayload(ts));
  });

  const aliceDb = getDb(testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }));
  await assertSucceeds(
    aliceDb.collection("orders").doc("ord_1").set({
      userId: ALICE_UID,
      customerName: "Alice",
      customerPhone: "9876543210",
      deliveryAddress: "22 Rose Avenue, Mumbai",
      productId: "prod_1",
      productName: "Rose Silk Gown",
      selectedSize: "M",
      selectedColor: "Blush Pink",
      quantity: 1,
      unitPrice: 4999,
      totalPrice: 4999,
      status: "Pending",
      channel: "WhatsApp Enquiry",
      notes: "",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Customer cannot self-confirm order status
  await assertFails(
    aliceDb.collection("orders").doc("ord_1").update({
      status: "Confirmed",
      updatedAt: pastTimestamp(),
    })
  );

  // Customer CAN cancel Pending order
  await assertSucceeds(
    aliceDb.collection("orders").doc("ord_1").update({
      status: "Cancelled",
      updatedAt: pastTimestamp(),
    })
  );

  // Unfiltered list fails; filtered list succeeds
  await assertFails(aliceDb.collection("orders").get());
  await assertSucceeds(aliceDb.collection("orders").where("userId", "==", ALICE_UID).get());

  // Cross-user read rejected
  const bobDb = getDb(testEnv.authenticatedContext(BOB_UID, { email: "bob@example.com", email_verified: true }));
  await assertFails(bobDb.collection("orders").doc("ord_1").get());
});

test("Wishlist: strictly isolated per user", async () => {
  const aliceDb = getDb(testEnv.authenticatedContext(ALICE_UID));
  const bobDb = getDb(testEnv.authenticatedContext(BOB_UID));

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("wishlist").doc("prod_1").set({
      productId: "prod_1",
      productName: "Rose Silk Gown",
      price: 4999,
      createdAt: pastTimestamp(),
    })
  );

  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("wishlist").get()
  );
});
