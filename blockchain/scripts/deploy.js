const hre = require("hardhat");
const fs = require("fs");
const path = require("path");

async function main() {
  const parkingFee = hre.ethers.parseEther("0.002");
  const ParkingPayment = await hre.ethers.getContractFactory("ParkingPayment");
  const contract = await ParkingPayment.deploy(parkingFee);

  await contract.waitForDeployment();

  const address = await contract.getAddress();
  console.log("ParkingPayment deployed to:", address);
  console.log("Parking fee:", hre.ethers.formatEther(parkingFee), "ETH");

  // Save contract address for the backend
  const backendDir = path.join(__dirname, '../../backend');

  const addressData = {
    address: address,
    parkingFee: parkingFee.toString(),
    network: "localhost",
    deployedAt: new Date().toISOString()
  };
  fs.writeFileSync(
    path.join(backendDir, 'contract-address.json'),
    JSON.stringify(addressData, null, 2)
  );
  console.log("Contract address saved to backend/contract-address.json");

  // Copy ABI for the backend
  const artifactPath = path.join(__dirname, '../artifacts/contracts/ParkingPayment.sol/ParkingPayment.json');
  const artifact = JSON.parse(fs.readFileSync(artifactPath, 'utf8'));
  fs.writeFileSync(
    path.join(backendDir, 'contract-abi.json'),
    JSON.stringify(artifact.abi, null, 2)
  );
  console.log("Contract ABI saved to backend/contract-abi.json");
}

main()
  .then(() => process.exit(0))
  .catch((error) => {
    console.error(error);
    process.exit(1);
  });
