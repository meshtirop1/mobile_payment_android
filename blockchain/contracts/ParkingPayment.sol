// SPDX-License-Identifier: MIT
pragma solidity ^0.8.19;

contract ParkingPayment {
    address public owner;
    uint256 public parkingFee;
    mapping(address => uint256) public balances;

    event Deposited(address indexed user, uint256 amount);
    event ParkingPaid(address indexed user, string plateNumber, uint256 fee);
    event Withdrawn(address indexed owner, uint256 amount);

    constructor(uint256 _parkingFee) {
        owner = msg.sender;
        parkingFee = _parkingFee;
    }

    modifier onlyOwner() {
        require(msg.sender == owner, "Only owner can call this");
        _;
    }

    // Deposit ETH to pay for parking later
    function deposit() external payable {
        require(msg.value > 0, "Must deposit something");
        balances[msg.sender] += msg.value;
        emit Deposited(msg.sender, msg.value);
    }

    // Pay for parking using deposited balance
    function payForParking(string calldata plateNumber) external {
        require(balances[msg.sender] >= parkingFee, "Insufficient balance");
        balances[msg.sender] -= parkingFee;
        emit ParkingPaid(msg.sender, plateNumber, parkingFee);
    }

    // View balance
    function getBalance(address user) external view returns (uint256) {
        return balances[user];
    }

    // Owner can update the parking fee
    function setParkingFee(uint256 _newFee) external onlyOwner {
        parkingFee = _newFee;
    }

    // Owner can withdraw collected fees
    function withdraw() external onlyOwner {
        uint256 contractBalance = address(this).balance;
        require(contractBalance > 0, "No funds to withdraw");
        payable(owner).transfer(contractBalance);
        emit Withdrawn(owner, contractBalance);
    }
}
