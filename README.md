business-customer
=============
Business customer microservice

[![Build Status](https://travis-ci.org/hmrc/business-customer.svg)](https://travis-ci.org/hmrc/business-customer) [ ![Download](https://api.bintray.com/packages/hmrc/releases/business-customer/images/download.svg) ](https://bintray.com/hmrc/releases/business-customer/_latestVersion)


This service privides the ability for uk-based or non-UK based agents, organisation or self-assessment individuals to create their Business Partner in ETMP. It also allows agents to add known-facts to enrol for a service in gateway.

### Create Business Partner

The request must be a valid json using one of the following uris
- POST    /sa/:utr/business-customer/register: Self Assessment users should call this
- POST    /org/:utr/business-customer/register: Organisations should call this
- POST    /agent/:utr/business-customer/register: Agents should call this

Where:

| Parameter | Message                      |
|:--------:|------------------------------|
|   utr    | The Unique Tax Reference or Agent-Code or Org-Id |

#### Example of usage for individual or Agent

 POST /agent/123456789/business-customer/register
 POST /sa/123456789/business-customer/register

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

 POST /org/123456789/business-customer/register

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

### Add Known Facts

The request must be a valid json using one of the following uris
- POST    /agent/:utr/business-customer/:serviceName/known-facts: Agents should call this

Where:

| Parameter | Message                      |
|:--------:|------------------------------|
|   utr    | The unique reference of user, for agents Agent-Code |
|   serviceName    | name of service against which person has to enrol |

####Example of usage

 POST /agent/123456789/business-customer/ATED/known-facts

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

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").

