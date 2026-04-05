const { expect } = require("chai");
const { ethers } = require("hardhat");

describe("ParkingPayment", function () {
  let contract, owner, user1;
  const parkingFee = ethers.parseEther("0.002");

  beforeEach(async function () {
    [owner, user1] = await ethers.getSigners();
    const ParkingPayment = await ethers.getContractFactory("ParkingPayment");
    contract = await ParkingPayment.deploy(parkingFee);
  });

  it("should accept deposits", async function () {
    const depositAmount = ethers.parseEther("0.01");
    await contract.connect(user1).deposit({ value: depositAmount });
    const balance = await contract.getBalance(user1.address);
    expect(balance).to.equal(depositAmount);
  });

  it("should emit Deposited event", async function () {
    const depositAmount = ethers.parseEther("0.01");
    await expect(contract.connect(user1).deposit({ value: depositAmount }))
      .to.emit(contract, "Deposited")
      .withArgs(user1.address, depositAmount);
  });

  it("should pay for parking", async function () {
    const depositAmount = ethers.parseEther("0.01");
    await contract.connect(user1).deposit({ value: depositAmount });
    await contract.connect(user1).payForParking("ABC1234");
    const balance = await contract.getBalance(user1.address);
    expect(balance).to.equal(depositAmount - parkingFee);
  });

  it("should emit ParkingPaid event", async function () {
    const depositAmount = ethers.parseEther("0.01");
    await contract.connect(user1).deposit({ value: depositAmount });
    await expect(contract.connect(user1).payForParking("ABC1234"))
      .to.emit(contract, "ParkingPaid")
      .withArgs(user1.address, "ABC1234", parkingFee);
  });

  it("should revert if insufficient balance", async function () {
    await expect(contract.connect(user1).payForParking("ABC1234"))
      .to.be.revertedWith("Insufficient balance");
  });

  it("should allow owner to update fee", async function () {
    const newFee = ethers.parseEther("0.005");
    await contract.setParkingFee(newFee);
    expect(await contract.parkingFee()).to.equal(newFee);
  });
});
