business-customer
================
Business customer microservice

[![Build Status](https://travis-ci.org/hmrc/business-customer.svg)](https://travis-ci.org/hmrc/business-customer) [ ![Download](https://api.bintray.com/packages/hmrc/releases/business-customer/images/download.svg) ](https://bintray.com/hmrc/releases/business-customer/_latestVersion)


This service provides the ability for Non-UK based agents, organisation or self-assessment individuals to create their Business Partner Record in ETMP (HOD). 
It also allows agents to add known-facts to enrol for a service in gateway.

### Register Non-UK clients and agents

The request must be a valid json using one of the following uris

| PATH | Supported Methods | Description |
|------|-------------------|-------------|
|```/sa/:sa/business-customer/register``` | POST | registers a SA user|
|```/org/:org/business-customer/register``` | POST | registers an Org user |
|```/agent/:ac/business-customer/register``` | POST | registers an agent |

Where:

| Parameter | Message |
|--------|------------------------------|
|   sa/org/ac   | unique auth identifier for clients/agents |

Response status codes:

| Status | Message     |
|-------|-------------|
| 200   | Ok          |
| 400   | Bad Request |
| 404   | Not Found   |
| 500   | Internal Server Error |
| 503   | Service Unavailable |


#### Example of usage for individual or Agent

 ```POST /agent/123456789/business-customer/register```
  
  ```POST /sa/123456789/business-customer/register```

 **Request body**

 ```json
{
  "acknowledgmentReference": "12345678901234567890123456789012",
    "isAnAgent": false,
    "isAGroup": false,
    "identification": {
    "idNumber": "123456",
    "issuingInstitution": "France Institution",
    "issuingCountryCode": "FR"
  },
  "individual": {
    "firstName": "Joe",
    "lastName": "Blogg",
    "dateOfBirth": "1990-04-03"
  },
  "address": {
    "addressLine1": "address-line-1",
    "addressLine2": "address-line-2",
    "addressLine3": "address-line-3",
    "addressLine4": "Newcastle",
    "postalCode": "AB1 4CD",
    "countryCode": "GB"
  },
  "contactDetails": {
    "phoneNumber": "01234567890",
    "mobileNumber": "07712345678",
    "faxNumber": "01234567891"
  }
}
 ```
 **Response body**

 ```json
{
  "processingDate":"2001-12-17T09:30:47Z",
  "sapNumber": "1234567890",
  "safeId": "XE0001234567890"
}
 ```

#### Example of usage for organisation

 ```POST /org/123456789/business-customer/register```

 **Request body**

 ```json
{
  "acknowledgmentReference": "12345678901234567890123456789012",
  "isAnAgent": false,
  "isAGroup": false,
  "identification": {
    "idNumber": "123456",
    "issuingInstitution": "France Institution",
    "issuingCountryCode": "FR"
  },
  "organisation": {
    "organisationName": "ACME Limited"
  },
  "address": {
    "addressLine1": "address-line-1",
    "addressLine2": "address-line-2",
    "addressLine3": "address-line-3",
    "addressLine4": "Newcastle",
    "postalCode": "AB1 4CD",
    "countryCode": "GB"
  },
  "contactDetails": {
    "phoneNumber": "01234567890",
    "mobileNumber": "07712345678",
    "faxNumber": "01234567891"
  }
}
 ```
 **Response body**

 ```json
{
  "processingDate":"2001-12-17T09:30:47Z",
  "sapNumber": "1234567890",
  "safeId": "XE0001234567890"
}
 ```
### Update Registration for all clients and agents

The request must be a valid json using one of the following uris

| PATH | Supported Methods | Description |
|------|-------------------|-------------|
|```/sa/:sa/business-customer/update/:safeId ``` | POST | registers a SA user|
|```/org/:org/business-customer/update/:safeId ``` | POST | registers an Org user |
|```/agent/:ac/business-customer/update/:safeId ``` | POST | registers an agent |

Where:

| Parameter | Message |
|--------|------------------------------|
|   sa/org/ac    | unique auth id for clients/agents |
|   safeId    | ID generated when registered in ETMP - Register Once in ROSM (Register Once Subscribe Many) pattern |

Response status codes:

| Status | Message     |
|-------|-------------|
| 200   | Ok          |
| 400   | Bad Request |
| 404   | Not Found   |
| 500   | Internal Server Error |
| 503   | Service Unavailable |

#### Example of usage for individual or Agent

 ```POST /agent/123456789/business-customer/update/XE0001234567890```
 
  ```POST /sa/123456789/business-customer/update/XE0001234567890```

 **Request body**

 ```json
{
	"acknowledgementReference": "12345678901234567890123456789012",
	"isAnAgent": false,
	"isAGroup": false,
	"identification": {
		"idNumber": "123456",
		"issuingInstitution": "France Institution",
		"issuingCountryCode": "FR"
	},
	"individual": {
		"firstName": "John",
		"lastName": "Smith",
		"dateOfBirth": "1990-04-03"
	},
	"address": {
		"addressLine1": "100, Sutton Street",
		"addressLine2": "Wokingham",
		"addressLine3": "Surrey",
		"addressLine4": "London",
		"postalCode": "DH1 4EJ",
		"countryCode": "GB"
	},
	"contactDetails": {
		"phoneNumber": "01332752856",
		"mobileNumber": "07782565326",
		"faxNumber": "01332754256"
	}
}
 ```
 
```text
isAnAgent = true, for an agent
``` 
 
 **Response body**

 ```json
{
	"processingDate": "2001-12-17T09:30:47Z",
	"sapNumber": "1234567890",
	"safeId": "XE0001234567890"
}
 ```

#### Example of usage for organisation

 ```POST /org/123456789/business-customer/update/XE0001234567890```

 **Request body**

 ```json
{
	"acknowledgementReference": "12345678901234567890123456789012",
	"isAnAgent": false,
	"isAGroup": false,
	"identification": {
		"idNumber": "123456",
		"issuingInstitution": "France Institution",
		"issuingCountryCode": "FR"
	},
	"organisation": {
		"organisationName": "John"
	},
	"address": {
		"addressLine1": "100, Sutton Street",
		"addressLine2": "Wokingham",
		"addressLine3": "Surrey",
		"addressLine4": "London",
		"postalCode": "DH1 4EJ",
		"countryCode": "GB"
	},
	"contactDetails": {
		"phoneNumber": "01332752856",
		"mobileNumber": "07782565326",
		"faxNumber": "01332754256"
	}
}
 ```
 **Response body**

 ```json
{
  "processingDate":"2001-12-17T09:30:47Z",
  "sapNumber": "1234567890",
  "safeId": "XE0001234567890"
}
 ```

### Add Known Facts

The request must be a valid json using one of the following uris

| PATH | Supported Methods | Description |
|------|-------------------|-------------|
|```/agent/:ac/business-customer/:serviceName/known-facts``` | POST | agents adds known-facts |

Where:

| Parameter | Message |
|--------|------------------------------|
|   ac    | unique auth identifier for agents|
|   serviceName    | name of service for which agent has to enrol |

Response status codes:

| Status | Message     |
|-------|-------------|
| 200   | Ok          |

#### Example of usage

 ```POST /agent/123456789/business-customer/ATED/known-facts```

 **Request body**

 ```json
{
  "facts":[
    {
      "type":"AgentRefNumber",
      "value":"AARN1234567"
    },
    {
      "type":"SAFEID",
      "value":"XE0001234567890"
    }
  ]
}
 ```
 **Response body**

 ```json
{
  "linesUpdated":1
}
 ```

### License


This code is open source software licensed under the [Apache 2.0 License].

[Apache 2.0 License]: http://www.apache.org/licenses/LICENSE-2.0.html

