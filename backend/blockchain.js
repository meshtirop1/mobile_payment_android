const { ethers } = require('ethers');
const fs = require('fs');
const path = require('path');
const http = require('http');

let provider = null;
let contract = null;
let signer = null;
let isConnected = false;

// Quick check if a port is open (no retries)
function checkPort(host, port, timeoutMs) {
  return new Promise((resolve) => {
    const req = http.request({ host, port, method: 'POST', timeout: timeoutMs,
      headers: { 'Content-Type': 'application/json' }
    }, (res) => {
      res.resume();
      resolve(true);
    });
    req.on('error', () => resolve(false));
    req.on('timeout', () => { req.destroy(); resolve(false); });
    req.write(JSON.stringify({ jsonrpc: '2.0', method: 'eth_blockNumber', params: [], id: 1 }));
    req.end();
  });
}

async function initBlockchain() {
  try {
    // Quick check if Hardhat/Ganache is running - no retries
    const portOpen = await checkPort('127.0.0.1', 8545, 2000);
    if (!portOpen) {
      console.log('Blockchain: Local node not running on port 8545. Crypto payments disabled.');
      console.log('  To enable: cd blockchain && npx hardhat node');
      isConnected = false;
      return false;
    }

    // Connect to local Hardhat/Ganache node (disable polling to prevent spam)
    provider = new ethers.JsonRpcProvider('http://127.0.0.1:8545', undefined, {
      staticNetwork: true
    });

    // Verify connection
    await provider.getBlockNumber();

    // Load contract address and ABI
    const addressPath = path.join(__dirname, 'contract-address.json');
    const abiPath = path.join(__dirname, 'contract-abi.json');

    if (!fs.existsSync(addressPath) || !fs.existsSync(abiPath)) {
      console.log('Blockchain: Contract not deployed yet. Crypto payments disabled.');
      console.log('  Run: cd blockchain && npx hardhat node (terminal 1)');
      console.log('  Run: cd blockchain && npx hardhat run scripts/deploy.js --network localhost (terminal 2)');
      return false;
    }

    const { address } = JSON.parse(fs.readFileSync(addressPath, 'utf8'));
    const abi = JSON.parse(fs.readFileSync(abiPath, 'utf8'));

    // Use the first account from local node as the backend signer
    signer = await provider.getSigner(0);
    contract = new ethers.Contract(address, abi, signer);

    isConnected = true;
    console.log('Blockchain: Connected to local network');
    console.log('Blockchain: Contract at', address);
    return true;
  } catch (err) {
    console.log('Blockchain: Local node not available. Crypto payments disabled.');
    console.log('  Start Hardhat node: cd blockchain && npx hardhat node');
    provider = null;
    isConnected = false;
    return false;
  }
}

function isBlockchainConnected() {
  return isConnected;
}

// Get a signer for a specific account index (simulating different users)
async function getUserSigner(accountIndex) {
  if (!provider) throw new Error('Blockchain not connected');
  return provider.getSigner(accountIndex);
}

// Deposit ETH for a user (using their account index as identity)
async function depositForUser(accountIndex, amountEth) {
  if (!isConnected) throw new Error('Blockchain not connected');
  const userSigner = await provider.getSigner(accountIndex);
  const userContract = contract.connect(userSigner);
  const tx = await userContract.deposit({
    value: ethers.parseEther(amountEth.toString())
  });
  const receipt = await tx.wait();
  return {
    txHash: receipt.hash,
    blockNumber: receipt.blockNumber
  };
}

// Pay for parking via smart contract
async function payForParkingCrypto(accountIndex, plateNumber) {
  if (!isConnected) throw new Error('Blockchain not connected');
  const userSigner = await provider.getSigner(accountIndex);
  const userContract = contract.connect(userSigner);

  // Check balance first
  const balance = await contract.getBalance(userSigner.address);
  const fee = await contract.parkingFee();

  if (balance < fee) {
    throw new Error(`Insufficient crypto balance. Have: ${ethers.formatEther(balance)} ETH, Need: ${ethers.formatEther(fee)} ETH`);
  }

  const tx = await userContract.payForParking(plateNumber);
  const receipt = await tx.wait();

  // Parse ParkingPaid event
  const event = receipt.logs.find(log => {
    try {
      const parsed = contract.interface.parseLog(log);
      return parsed && parsed.name === 'ParkingPaid';
    } catch { return false; }
  });

  return {
    txHash: receipt.hash,
    blockNumber: receipt.blockNumber,
    fee: ethers.formatEther(fee)
  };
}

// Get crypto balance for a user
async function getCryptoBalance(accountIndex) {
  if (!isConnected) throw new Error('Blockchain not connected');
  const userSigner = await provider.getSigner(accountIndex);
  const balance = await contract.getBalance(userSigner.address);
  return ethers.formatEther(balance);
}

// Get the wallet address for an account index
async function getWalletAddress(accountIndex) {
  if (!provider) throw new Error('Blockchain not connected');
  const userSigner = await provider.getSigner(accountIndex);
  return userSigner.address;
}

module.exports = {
  initBlockchain,
  isBlockchainConnected,
  depositForUser,
  payForParkingCrypto,
  getCryptoBalance,
  getWalletAddress
};
