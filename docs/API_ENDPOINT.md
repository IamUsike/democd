# Transaction Monitoring System

## API Endpoint Documentation

**Version:** 1.0

**Project:** Transaction Monitoring & Alert Dashboard

**Prepared By:** <Your Name>

**Date:** 02 August 2026




## Table of Contents

1. Introduction
2. API Standards
3. Transaction APIs
    - 3.1 Record Transaction
    - 3.2 Get All Transactions
    - 3.3 Get Transaction By ID
4. Alert APIs
5. Dashboard APIs
6. Rule APIs
7. HTTP Status Codes
8. Standard Error Response
9. API Versioning
10. Assumptions & Future Enhancements

---

# 1. Introduction

This document defines the REST API endpoints for the Transaction Monitoring System.

The purpose of this document is to provide a clear contract between the frontend application, backend services, and other internal services. It describes each API endpoint, its purpose, request format, response format, HTTP methods, validation rules, and possible status codes.

This document is based on the current MVP (Minimum Viable Product) architecture.



---

# 2. API Standards

## Data Format

- All requests must be sent in JSON format.
- All responses are returned in JSON format.
- UTF-8 encoding is used for all API communication.

---

## Resource Naming Convention

The APIs follow RESTful naming conventions.

Guidelines:

- Resource names use plural nouns.
- HTTP methods define the operation.
- URLs should not contain verbs.

Examples

| Operation | Endpoint |
|------------|----------|
| Create Transaction | POST /transactions |
| Get Transactions | GET /transactions |
| Get Transaction | GET /transactions/{transactionId} |
| Update Alert Status | PATCH /alerts/{alertId}/status |

---

## Authentication

Version 1 (MVP)

Authentication and authorization are outside the scope of the MVP.

Future versions will use JWT Bearer Token authentication.

---

## HTTP Methods

| Method | Purpose |
|----------|----------|
| GET | Retrieve resources |
| POST | Create new resources |
| PUT | Replace an entire resource |
| PATCH | Partially update an existing resource |
| DELETE | Remove resources (Not used in MVP) |

---

## Standard Request Headers

| Header | Required | Description |
|----------|----------|-------------|
| Content-Type | Yes | application/json |
| Accept | Yes | application/json |
| Authorization | No (Future) | JWT Bearer Token |

---

## Standard Success Response

```json
{
  "success": true,
  "message": "Operation completed successfully.",
  "data": {}
}

```
## Standard Error Response

```json
{
  "success": false,
  "message": "Validation failed.",
  "errors": []
}
```

---

# 3. Transaction APIs

The Transaction APIs are responsible for recording and retrieving financial transactions. Every new transaction is evaluated by the Rule Engine. If any monitoring rule is triggered, an alert is created by the Alert Service.

## 3.1 Record Transaction

### Purpose

Records a new financial transaction in the system.

After the transaction is successfully saved, it is synchronously evaluated by the Rule Engine. If any configured rule is triggered, the Rule Engine requests the Alert Service to create an alert.

### Endpoint

```
POST /api/v1/transactions
```

### HTTP Method

POST

### Called By

Web UI

### Description

Creates a new transaction and starts the transaction monitoring process.



### Request Headers

| Header | Required | Value |
|----------|----------|---------|
| Content-Type | Yes | application/json |
| Authorization | No (MVP) | JWT Token (Future) |


### Request Body

```json
{
  "senderAccountNumber": "ACC1001",
  "receiverAccountNumber": "ACC2001",
  "amount": 25000,
  "currency": "INR",
  "transactionType": "TRANSFER",
  "description": "Vendor Payment"
}
```
### Validation Rules

| Field | Validation |
|---------|-------------|
| senderAccountNumber | Required |
| receiverAccountNumber | Required |
| amount | Must be greater than 0 |
| currency | Required |
| transactionType | Required |

### Success Response

**HTTP Status:** 201 Created

```json
{
  "success": true,
  "message": "Transaction recorded successfully.",
  "data": {
    "transactionId": 101,
    "status": "SUCCESS"
  }
}
```
### Error Responses

#### 400 Bad Request

```json
{
  "success": false,
  "message": "Amount must be greater than zero."
}
```

#### 404 Not Found

```json
{
  "success": false,
  "message": "Sender account not found."
}
```

#### 500 Internal Server Error

```json
{
  "success": false,
  "message": "Unexpected server error."
}
```

### Processing Flow

1. Client sends a transaction request.
2. Transaction Service validates the request.
3. Transaction is stored in the database.
4. Rule Engine evaluates the transaction.
5. If a rule is triggered, the Rule Engine calls the internal Alert API.
6. Alert Service creates a new alert.
7. Response is returned to the client.

---


### Notes

- Every transaction is evaluated by the Rule Engine immediately after it is saved.
- If a monitoring rule is triggered, an alert is automatically created.
- A transaction is successfully recorded even if an alert is generated.



## 3.2 Get All Transactions

### Purpose

Retrieves a list of all recorded transactions available in the system.

This endpoint is primarily used by the Web UI to display transaction history and support transaction search and investigation.

### Endpoint

GET /api/v1/transactions

### HTTP Method

GET

### Called By

- Web UI
- Investigators (Future)

### Request Headers

| Header | Required | Value |
|----------|----------|---------|
| Content-Type | Yes | application/json |

### Request Parameters

None

### Success Response

HTTP Status: 200 OK

```json
{
  "success": true,
  "message": "Transactions retrieved successfully.",
  "data": [
    {
      "transactionId": 101,
      "senderAccountNumber": "ACC1001",
      "receiverAccountNumber": "ACC2002",
      "amount": 25000,
      "currency": "INR",
      "transactionType": "TRANSFER",
      "status": "SUCCESS"
    }
  ]
}
```

### Error Response

500 Internal Server Error

```json
{
  "success": false,
  "message": "Unable to retrieve transactions."
}
```



### Notes

- Returns all transactions currently available in the system.
- Pagination and filtering are not supported in Version 1.
---



## 3.3 Get Transaction By ID

### Purpose

Retrieves detailed information for a specific transaction using its unique transaction ID.

### Endpoint

GET /api/v1/transactions/{transactionId}

### HTTP Method

GET

### Path Parameters

| Parameter | Type | Description |
|------------|------|-------------|
| transactionId | Long | Unique transaction identifier |

### Success Response

HTTP Status: 200 OK

```json
{
  "success": true,
  "message": "Transaction retrieved successfully.",
  "data": {
      "transactionId":101,
      "senderAccountNumber":"ACC1001",
      "receiverAccountNumber":"ACC2002",
      "amount":25000,
      "currency":"INR",
      "transactionType":"TRANSFER",
      "status":"SUCCESS",
      "transactionDateTime":"2026-08-02T10:15:00"
  }
}
```

### Error Response

404 Not Found

```json
{
    "success":false,
    "message":"Transaction not found."
}
```
### Notes

- Returns all alerts generated by the Rule Engine.
- Includes both active and historical alerts.


---



# 4. Alert APIs

The Alert APIs are responsible for retrieving and managing alerts generated by the Rule Engine.

Alerts are created automatically when a transaction matches one or more monitoring rules. Investigators use these APIs to review alerts and update their investigation status.



## 4.1 Get All Alerts

### Purpose

Retrieves all alerts generated by the Transaction Monitoring System.

This endpoint is used by investigators to view all active and historical alerts.

### Endpoint

```
GET /api/v1/alerts
```

### HTTP Method

GET

### Called By

- Web UI
- Investigator

### Request Parameters

None

### Success Response

HTTP Status: **200 OK**

```json
{
  "success": true,
  "message": "Alerts retrieved successfully.",
  "data": [
    {
      "alertId": 201,
      "transactionId": 101,
      "severity": "HIGH",
      "status": "OPEN",
      "ruleTriggered": "Amount Threshold Rule",
      "createdAt": "2026-08-02T10:20:00"
    }
  ]
}
```

### Error Response

```json
{
  "success": false,
  "message": "Unable to retrieve alerts."
}
```

### Notes

- Returns all alerts generated by the Rule Engine.
- Includes both active and historical alerts.



## 4.2 Get Alert Details

### Purpose

Retrieves complete information about a specific alert.

### Endpoint

```
GET /api/v1/alerts/{alertId}
```

### HTTP Method

GET

### Path Parameters

| Parameter | Type | Description |
|------------|------|-------------|
| alertId | Long | Unique Alert Identifier |

### Success Response

```json
{
  "success": true,
  "message": "Alert retrieved successfully.",
  "data": {
    "alertId": 201,
    "transactionId": 101,
    "severity": "HIGH",
    "status": "OPEN",
    "ruleTriggered": "Amount Threshold Rule",
    "assignedTo": null,
    "createdAt": "2026-08-02T10:20:00"
  }
}
```

### Error Response

```json
{
  "success": false,
  "message": "Alert not found."
}
```



### Notes

- Returns complete details for the selected alert.




## 4.3 Update Alert Status

### Purpose

Updates the current investigation status of an alert.

### Endpoint

```
PATCH /api/v1/alerts/{alertId}/status
```

### HTTP Method

PATCH

### Called By

Investigator

### Path Parameters

| Parameter | Type | Description |
|------------|------|-------------|
| alertId | Long | Alert Identifier |

### Request Body

```json
{
  "status": "INVESTIGATING"
}
```

### Allowed Status Values

| Status |
|----------|
| OPEN |
| ACKNOWLEDGED |
| INVESTIGATING |
| CLOSED |
| DISMISSED |

### Success Response

```json
{
  "success": true,
  "message": "Alert status updated successfully."
}
```

### Error Response

```json
{
  "success": false,
  "message": "Invalid alert status."
}




```

### Notes

- Only the status of an alert can be updated.
- Allowed status values are OPEN, ACKNOWLEDGED, INVESTIGATING, CLOSED and DISMISSED.---



# 5. Dashboard APIs

The Dashboard APIs provide summarized information required by investigators and administrators to monitor the health of the transaction monitoring system.

These APIs return aggregated data rather than individual transaction or alert records.



## 5.1 Dashboard Summary

### Purpose

Returns a summarized view of the Transaction Monitoring System.

This API is used to display dashboard cards containing overall system statistics.

### Endpoint

```
GET /api/v1/dashboard
```

### HTTP Method

GET

### Called By

- Web UI

### Success Response

HTTP Status: **200 OK**

```json
{
  "success": true,
  "message": "Dashboard summary retrieved successfully.",
  "data": {
    "totalTransactions": 10254,
    "totalAlerts": 156,
    "openAlerts": 18,
    "closedAlerts": 132,
    "highSeverityAlerts": 6
  }
}
```

### Error Response

```json
{
  "success": false,
  "message": "Unable to retrieve dashboard summary."
}
```

### Notes

This endpoint provides aggregated information used to populate dashboard summary cards.



## 5.2 Dashboard Statistics

### Purpose

Returns statistical data required to display dashboard charts and reports.

### Endpoint

```
GET /api/v1/dashboard/statistics
```

### HTTP Method

GET

### Called By

- Web UI

### Success Response

```json
{
  "success": true,
  "message": "Statistics retrieved successfully.",
  "data": {
    "transactionsPerDay": [
      {
        "date": "2026-08-01",
        "count": 124
      },
      {
        "date": "2026-08-02",
        "count": 182
      }
    ]
  }
}
```
### Notes



This endpoint provides statistical information used to render dashboard charts and reports.
---

# 6. Rule APIs

The Rule APIs provide visibility into the transaction monitoring rules configured in the system.

For the MVP release, rules are hardcoded and cannot be created, modified, or deleted through the user interface.


## 6.1 Get Configured Rules

### Purpose

Returns all monitoring rules currently configured in the Rule Engine.

### Endpoint

```
GET /api/v1/rules
```

### HTTP Method

GET

### Called By

- Web UI

### Success Response

```json
{
  "success": true,
  "message": "Rules retrieved successfully.",
  "data": [
    {
      "ruleId": 1,
      "ruleName": "Amount Threshold Rule",
      "thresholdAmount": 10000,
      "severity": "HIGH",
      "status": "ACTIVE"
    }
  ]
}
```
### Notes

- Rules are read-only in the MVP.
- Rules cannot be created, updated or deleted through the user interface.
---




# 7. HTTP Status Codes

The following HTTP status codes are used by the Transaction Monitoring System APIs.

| Status Code | Meaning | Usage |
|--------------|---------|-------|
| 200 OK | Request completed successfully | GET, PATCH |
| 201 Created | Resource created successfully | POST |
| 400 Bad Request | Invalid request or validation failed | Invalid input |
| 404 Not Found | Requested resource does not exist | Invalid Transaction ID or Alert ID |
| 500 Internal Server Error | Unexpected server error | Database or application failure |




---

# 8. Standard Error Response

All APIs return a standard error response when an operation fails.

## Example Response

```json
{
  "success": false,
  "message": "Validation failed.",
  "errors": [
    "Amount must be greater than zero."
  ]
}
```

### Response Fields

| Field | Description |
|---------|-------------|
| success | Indicates whether the API call was successful |
| message | High-level description of the error |
| errors | List of validation or business errors |


---

# 9. API Versioning

The Transaction Monitoring System follows URL-based API versioning.

Current Version

```
/api/v1
```

Future releases may introduce newer API versions without affecting existing clients.

Example

```
/api/v2/transactions
```



---

# 10. Assumptions & Future Enhancements

## Assumptions

- Transactions are received through REST APIs.
- Monitoring rules are evaluated immediately after transaction creation.
- Alerts are generated automatically when a monitoring rule is triggered.
- Rule configuration is hardcoded for the MVP release.
- Authentication and authorization are outside the scope of Version 1.

## Future Enhancements

- JWT-based authentication
- User and role management
- Dynamic rule configuration
- Alert assignment to investigators
- Investigation notes
- Audit trail
- Notification service (Email/SMS)
- Advanced dashboard analytics

