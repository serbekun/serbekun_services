# REST endpoints for api

## links

```
/v0/api/catalog/links
```

---

#### GET

get links catalog

- Request

```
/v0/api/catalog/links
```

---

- Response (Example) 200
```json
{
  "a264e499-5fa9-4bfe-9996-0b51136cc3f9" : {
    "uuid" : "a264e499-5fa9-4bfe-9996-0b51136cc3f9",
    "url" : "https://youtube.com",
    "name" : "youtube",
    "description" : "youtube index page"
  },
  "f2a2f5d0-99d5-4d09-9a77-b7576b0acd1f" : {
    "uuid" : "f2a2f5d0-99d5-4d09-9a77-b7576b0acd1f",
    "url" : "https://github.com",
    "name" : "github",
    "description" : "github index page"
  }
}
```

---

#### POST

upload new link

- Request
```json
{
    "url": "https://example.com",
    "name": "example web site",
    "description": "domain that write at docs"
}
```

##### Response

- 201 successfully uploaded
```json
{
    "token": "123"
}
```

- 400 error upload
```json
{
  "error": "INVALID_REQUEST",
  "message": "required fields are missing"
}
```

---

#### PUT

```
/v0/api/catalog/links/{uuid}
```

update exits link

- Request
```json
{
    "token": "123", // link access token 
    "url": "https://new-example.com/",
    "name": "new example com",
    "description": "new example com"
}
```

##### Response

- 204
```
nothing
```

- 404
```json
{
    "error": "NOT_FOUND",
    "details": "uuid that you want to update don't exist"
}
```

#### DELETE

```
/v0/api/catalog/links/{uuid}
```

- Request
```json
{
    "uuid": "a264e499-5fa9-4bfe-9996-0b51136cc3f9",
    "token": "123"
}
```

---

## programs catalog

### /v0/api/catalog/programs

#### GET

get program catalog list

- Request
```
/v0/api/catalog/programs
```

- Response
```json
{
    "program1.exe": {
        "description": "some description of program",
        "size": 65432 // in bytes
    },
    "game1.deb": {
        "description": "some description",
        "size": 13212412
    }
}
```

#### GET

- Request

```
/v0/api/catalog/programs/{name}
```

##### Response

- 200
Program binary file

- 404
program file not found
```json
{
    "error": "NOT_FOUND",
    "message": "program not found"
}
```

## Cipher API

```
/v0/api/cipher/aes
```

### GET

get generate key

- Request
/v0/api/cipher/aes

- Response
```json
{
    "key": "a2V5" // base64 encoded AES key
}
```

### POST

encrypt data

```
/v0/api/cipher/aes/encrypt
```

- Request
```json
{
    "data": "ZGF0YQo=", // base64 encoded data
    "key": "a2V5"
}
```

#### Responses

- 200
```json
{
    "data": "ZGF0YQo=" // base64 encode encrypted data
}
```

- 400 error make request
```json
{
  "error": "INVALID_REQUEST",
  "message": "required fields are missing"
}
```

### POST

decrypt

```
/v0/api/cipher/aes/decrypt
```

- Request
```json
{
    "data": "ZGF0YQo=",
    "key": "a2V5"
}
```

#### Responses

- 200
```json
{
    "data": "ZGF0YQo="
}
```

- 400 error make request
```json
{
    "error": "INVALID_REQUEST",
    "message": "required fields are missing"
}
```