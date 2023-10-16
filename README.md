# Simple Carina QA application

This project contains a web service with the following endpoints under http://localhost:8080

* POST `/data/load`
* GET `/data/count`
* POST `/data/delete`
* GET `/qa`

The `/qa` endpoint takes a `question` parameter which is the question you want to ask the AI model.
The `/qa` endpoint also takes a `stuffit` boolean parameter, whose default it true, that will 'stuff the prompt' with
similar documents to the question.  When stuffing the prompt, this follows the RAG pattern.

## Prerequisites

### OpenAI Credentials

Create an account at [OpenAI Signup](https://platform.openai.com/signup) and generate the token at [API Keys](https://platform.openai.com/account/api-keys).

The Spring AI project defines a configuration property named `spring.ai.openai.api-key` that you should set to the value of the `API Key` obtained from `openai.com`.

You can set this in the projects `/resources/application.yml` file or by exporting an environment variable, for example.
```shell
export SPRING_AI_OPENAI_API_KEY=<INSERT KEY HERE>
```

The `/resources/application.yml` references the environment variable `${OPENAI_API_KEY}` as that is what the onboarding instructions for OpenAI suggest.

In short, Spring Boot provides many ways to set this property, pick a method that works for your needs.

## VectorStore

To run the PgVectorStore locally, using docker-compose.
From the top project directory and run:

```
docker-compose up
```

Later starts Postgres DB on localhost and port 5432.

Then you can connect to the database (password: `postgres`) and inspect or alter the `vector_store` table content:

```
psql -U postgres -h localhost -p 5432

\l
\c postgres
\dt

select count(*) from vector_store;

delete from vector_store;
```

The UI tool [DBeaver](https://dbeaver.io/download/) is also a useful GUI for postgres.

## Building and running

```
./mvnw spring-boot:run
```

## Access the endpoints

The first thing you should do is load the data.  The examples show usage with the [HTTPie](https://httpie.io/) command line utility as it simplifies sending HTTP requests with data as compared to curl.

### Loading, counting and deleting data

```shell 
http POST http://localhost:8080/data/load
```

Next you can see how many document fragments were loaded into the Vector Store using

```shell
http http://localhost:8080/data/count
```
If you want to start over, for example because you changed in the code which document is being loaded, then execute

```shell
http POST http://localhost:8080/data/delete
```

### Q&A over the document

Send you question to the AI Model using

```shell
http --body --unsorted localhost:8080/qa message==<insert question here>
```

Note that there are two equal signs `==` , that separate the key-value pairs

To ask the same question but without the similar documents stuffing the prompt, that is, not using the RAG pattern,

```shell
http --body --unsorted http://localhost:8080/qa message==<insert question here> stuffit==false
```

#### Examples

```shell
$ http --body --unsorted localhost:8080/qa question=="What is the purpose of Carina?"
{
    "question": "What is the purpose of Carina?",
    "answer": "The purpose of Carina is to provide a safe and easy-to-use online care matching service. It aims to connect care providers with individuals and families who are in need of home care or child care services. Carina prioritizes building community and supporting care workers by bringing good jobs to them. Its goal is to strengthen the care economy and support workers, individuals, and families in the process."
}
```

and without stuffing the prompt

```shell
$ http --body --unsorted localhost:8080/qa question=="What is the purpose of Carina?" stuffit==false
{
    "question": "What is the purpose of Carina?",
    "answer": "Carina is a constellation located in the southern sky. It does not have a specific purpose, but like other constellations, it serves as a way to organize and identify stars in the night sky. Constellations have been used for navigation, storytelling, and scientific observation throughout history."
}

```
