package com.azure.cosmos.sample.common;

import java.util.List;

public class GetItemsProcedureResponse {
    public List<Address> result;

    public String continuation;

    public List<Address> getResult() {
        return this.result;
    }

    public void setResult(List<Address> result) {
        this.result = result;
    }

    public String getContinuation() {
        return this.continuation;
    }

    public void setContinuation(String continuation) {
        this.continuation = continuation;
    }
}
