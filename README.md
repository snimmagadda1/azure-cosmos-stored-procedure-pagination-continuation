# Example implementation of an Azure Cosmos DB stored procedure with pagination

When working with large data sets or long running processes in Cosmos, you should use [continuation](https://learn.microsoft.com/en-us/azure/cosmos-db/nosql/query/pagination). Considering stored procedures have a 5 second timeout and are resource-limited, a robust stored procedure implementation must be used to return the continuation token to the client. This is a quick re-wiring of the [java CRUD quickstart](https://github.com/Azure-Samples/azure-cosmos-java-sql-api-samples/blob/main/src/main/java/com/azure/cosmos/examples/crudquickstart/sync/SampleCRUDQuickstart.java) to query all items in a container with a continuation token.

The stored procedure is written using the [JavaScript query api](https://learn.microsoft.com/en-us/azure/cosmos-db/nosql/javascript-query-api). It can be found at [get-items-continuation.js](./get-items-continuation.js).

The calling of the stored procedure `while continuation != null` is orchestrated by Java client code in `com.azure.cosmos.sample.sync.SyncMain`.

### 🏠 [Description](https://s11a.com/writing-a-cosmos-db-stored-procedure-with-continuation)

### Prerequisites

* Before you can run this sample, you must have the following prerequisites:

  * An active Azure account. If you don't have one, you can sign up for a [free account](https://azure.microsoft.com/free/). Alternatively, you can use the [Azure Cosmos DB Emulator](https://azure.microsoft.com/documentation/articles/documentdb-nosql-local-emulator) for this tutorial. As the emulator https certificate is self signed, you need to import its certificate to the java trusted certificate store as [explained here](https://docs.microsoft.com/azure/cosmos-db/local-emulator-export-ssl-certificates).

  * JDK 1.8+
  * Maven

### Quickstart

* First clone this repository using

```bash
git clone https://github.com/snimmagadda1/azure-cosmos-stored-procedure-pagination-continuation.git
```

* From a command prompt or shell, run the following command to compile and resolve dependencies.

```bash
cd azure-cosmos-stored-procedure-pagination-continuation
mvn clean package
```

* From a command prompt or shell, run the following command to run the application.

```bash
mvn exec:java -Dexec.mainClass="com.azure.cosmos.sample.sync.SyncMain"                                                    
```

Observe database, container, and item creation followed by paginated querying using a stored proc.
