const products = [
  { id: "p1", name: "Keyboard", sellerId: "s1" },
  { id: "p2", name: "Monitor", sellerId: "s2" },
  { id: "p3", name: "Mouse", sellerId: "s1" },
];

const sellers = new Map([
  ["s1", { id: "s1", displayName: "Acme Devices" }],
  ["s2", { id: "s2", displayName: "Northwind Tech" }],
]);

let sellerServiceCalls = 0;

function fetchSellerById(id) {
  sellerServiceCalls += 1;
  return sellers.get(id);
}

function fetchSellersByIds(ids) {
  sellerServiceCalls += 1;
  return ids.map((id) => sellers.get(id));
}

function naiveProductList() {
  sellerServiceCalls = 0;
  const result = products.map((product) => ({
    ...product,
    seller: fetchSellerById(product.sellerId),
  }));
  return { result, sellerServiceCalls };
}

function batchedProductList() {
  sellerServiceCalls = 0;
  const uniqueSellerIds = [...new Set(products.map((product) => product.sellerId))];
  const sellerResults = fetchSellersByIds(uniqueSellerIds);
  const sellerById = new Map(uniqueSellerIds.map((id, index) => [id, sellerResults[index]]));
  const result = products.map((product) => ({
    ...product,
    seller: sellerById.get(product.sellerId),
  }));
  return { result, sellerServiceCalls };
}

console.log("Naive resolver fanout:");
console.log(JSON.stringify(naiveProductList(), null, 2));
console.log("\nBatched resolver behavior:");
console.log(JSON.stringify(batchedProductList(), null, 2));