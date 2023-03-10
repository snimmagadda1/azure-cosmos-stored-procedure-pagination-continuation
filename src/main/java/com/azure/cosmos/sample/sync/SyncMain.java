// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.cosmos.sample.sync;

import com.azure.cosmos.ConsistencyLevel;
import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.CosmosException;
import com.azure.cosmos.CosmosStoredProcedure;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosDatabase;
import com.azure.cosmos.models.CosmosContainerProperties;
import com.azure.cosmos.models.CosmosItemRequestOptions;
import com.azure.cosmos.models.CosmosItemResponse;
import com.azure.cosmos.models.PartitionKey;
import com.azure.cosmos.models.CosmosStoredProcedureProperties;
import com.azure.cosmos.models.CosmosStoredProcedureRequestOptions;
import com.azure.cosmos.models.CosmosStoredProcedureResponse;
import com.azure.cosmos.models.CosmosContainerResponse;
import com.azure.cosmos.models.CosmosDatabaseResponse;
import com.azure.cosmos.models.ThroughputProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.azure.cosmos.sample.common.AccountSettings;
import com.azure.cosmos.sample.common.Address;
import com.azure.cosmos.sample.common.Families;
import com.azure.cosmos.sample.common.Family;
import com.azure.cosmos.sample.common.GetItemsProcedureResponse;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

public class SyncMain {

    private CosmosClient client;

    private final String databaseName = "ToDoList";
    private final String containerName = "Items";
    private final String storedProcName = "getIems";
    private final int MAX_QUERIES = 20;
    private final ObjectMapper mapper = new ObjectMapper();

    private CosmosDatabase database;
    private CosmosContainer container;
    private CosmosStoredProcedure storedProcedure;

    public void close() {
        client.close();
    }

    /**
     * Run a Hello CosmosDB console application.
     *
     * @param args command line args.
     */
    // <Main>
    public static void main(String[] args) {
        SyncMain p = new SyncMain();
        p.initClient();

        try {
            // From azure starter, create & seed
            p.getStartedDemo();

            // Run stored procedure logic
            p.runProcedureContinuation();
            System.out.println("Demo complete, please hold while resources are released");
            System.out.println("Running procedure to query items");

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(String.format("Cosmos getStarted failed with %s", e));
        } finally {
            System.out.println("Closing the client");
            p.close();
        }
        System.exit(0);
    }

    // </Main>

    private void initClient() {
        System.out.println("Using Azure Cosmos DB endpoint: " + AccountSettings.HOST);

        ArrayList<String> preferredRegions = new ArrayList<String>();
        preferredRegions.add("West US");

        // Create sync client
        client = new CosmosClientBuilder()
                .endpoint(AccountSettings.HOST)
                .key(AccountSettings.MASTER_KEY)
                .preferredRegions(preferredRegions)
                .userAgentSuffix("CosmosDBJavaQuickstart")
                .consistencyLevel(ConsistencyLevel.EVENTUAL)
                .buildClient();
    }

    private void getStartedDemo() throws Exception {

        createDatabaseIfNotExists();
        createContainerIfNotExists();
        scaleContainer();

        // Setup family items to create

        for (int i = 0; i < 300; i++) {
            ArrayList<Family> familiesToCreate = new ArrayList<>();
            familiesToCreate.add(Families.getAndersenFamilyItem());
            familiesToCreate.add(Families.getWakefieldFamilyItem());
            familiesToCreate.add(Families.getJohnsonFamilyItem());
            familiesToCreate.add(Families.getSmithFamilyItem());
            createFamilies(familiesToCreate);
        }

        createStoredProcedure();
    }

    private void createDatabaseIfNotExists() throws Exception {
        System.out.println("Create database " + databaseName + " if not exists.");

        // Create database if not exists
        CosmosDatabaseResponse databaseResponse = client.createDatabaseIfNotExists(databaseName);
        database = client.getDatabase(databaseResponse.getProperties().getId());

        System.out.println("Checking database " + database.getId() + " completed!\n");
    }

    private void createContainerIfNotExists() throws Exception {
        System.out.println("Create container " + containerName + " if not exists.");

        // Create container if not exists
        CosmosContainerProperties containerProperties = new CosmosContainerProperties(containerName, "/partitionKey");

        CosmosContainerResponse containerResponse = database.createContainerIfNotExists(containerProperties);
        container = database.getContainer(containerResponse.getProperties().getId());

        System.out.println("Checking container " + container.getId() + " completed!\n");
    }

    private void scaleContainer() throws Exception {
        System.out.println("Scaling container " + containerName + ".");

        try {
            // You can scale the throughput (RU/s) of your container up and down to meet the
            // needs of the workload. Learn more: https://aka.ms/cosmos-request-units
            ThroughputProperties currentThroughput = container.readThroughput().getProperties();
            int newThroughput = currentThroughput.getManualThroughput() + 100;
            container.replaceThroughput(ThroughputProperties.createManualThroughput(newThroughput));
            System.out.println("Scaled container to " + newThroughput + " completed!\n");
        } catch (CosmosException e) {
            if (e.getStatusCode() == 400) {
                System.err.println("Cannot read container throuthput.");
                System.err.println(e.getMessage());
            } else {
                throw e;
            }
        }
    }

    private void createFamilies(List<Family> families) throws Exception {
        double totalRequestCharge = 0;
        for (Family family : families) {

            // Create item using container that we created using sync client

            // Using appropriate partition key improves the performance of database
            // operations
            CosmosItemResponse item = container.createItem(family, new PartitionKey(family.getPartitionKey()),
                    new CosmosItemRequestOptions());

            // Get request charge and other properties like latency, and diagnostics
            // strings, etc.
            System.out.println(String.format("Created item with request charge of %.2f within" +
                    " duration %s",
                    item.getRequestCharge(), item.getDuration()));
            totalRequestCharge += item.getRequestCharge();
        }
        System.out.println(String.format("Created %d items with total request " +
                "charge of %.2f",
                families.size(),
                totalRequestCharge));
    }

    private void createStoredProcedure() throws Exception {
        CosmosStoredProcedureProperties definition = new CosmosStoredProcedureProperties(
                "getIems",
                Files.readString(Paths.get("get-items-continuation.js")));
        container
                .getScripts()
                .createStoredProcedure(definition);
    }

    private void runProcedureContinuation() throws Exception {
        storedProcedure = container
                .getScripts()
                .getStoredProcedure(storedProcName);

        List<Address> fetched = new ArrayList<>();
        GetItemsProcedureResponse response = runStoredProcedure(null);
        fetched.addAll(response.getResult());
        if (StringUtils.isNotBlank(response.getContinuation())) {
            String continuation = response.getContinuation();
            System.out.printf("Got initial response with continuation %s\n", continuation);
            int attempts = 0;
            while (StringUtils.isNotBlank(continuation) && attempts < MAX_QUERIES) {
                GetItemsProcedureResponse page = runStoredProcedure(continuation);
                fetched.addAll(page.getResult());
                continuation = page.getContinuation();
                System.out.printf("Got response with continuation %s\n", continuation);
                attempts++;
            }
            System.out.printf("Fetched %d addresses from collection in %d attempts\n",
                    fetched.size(),
                    attempts);
        }

    }

    private GetItemsProcedureResponse runStoredProcedure(String continuation) throws Exception {
        List<Object> input = new ArrayList<Object>();
        if (StringUtils.isNotBlank(continuation)) {
            input.add(continuation);
        }

        CosmosStoredProcedureRequestOptions options = new CosmosStoredProcedureRequestOptions();
        options.setPartitionKey(
                new PartitionKey("Anderson"));

        CosmosStoredProcedureResponse response = storedProcedure.execute(
                input,
                options);

        return mapper.readValue(response.getResponseAsString(),
                GetItemsProcedureResponse.class);
    }

}
