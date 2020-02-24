package be.blockchaindeveloper.chaincode;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.annotation.Contact;
import org.hyperledger.fabric.contract.annotation.Contract;
import org.hyperledger.fabric.contract.annotation.Default;
import org.hyperledger.fabric.contract.annotation.Info;
import org.hyperledger.fabric.contract.annotation.License;
import org.hyperledger.fabric.contract.annotation.Transaction;
import org.hyperledger.fabric.shim.Chaincode.Response;
import static org.hyperledger.fabric.shim.ResponseUtils.newSuccessResponse;
import org.hyperledger.fabric.shim.ledger.KeyModification;
import org.hyperledger.fabric.shim.ledger.KeyValue;

@Contract(name = "FishChaincode",
        info = @Info(title = "Simple CRUD Chaincode",
                description = "Very basic Java Chaincode example",
                version = "5",
                license
                = @License(name = "SPDX-License-Identifier: Apache-2.0",
                        url = ""),
                contact = @Contact(email = "dietjelle@gmail.com",
                        name = "FishChaincode",
                        url = "http://MyAssetContract.me")))
@Default
public class FishChaincode implements ContractInterface {

    public FishChaincode() {

    }

    /**
     * Init is called when initializing or updating chaincode. Use this to set
     * initial world state
     *
     * @param ctx
     * @return Response with message and payload
     */
    @Transaction
    public Response init(Context ctx) {

        //Adding one fish object to the ledger on init
        Fish fish = new Fish();
        fish.setId(UUID.randomUUID());
        fish.setPrice(BigDecimal.ONE);
        fish.setType("Salmon");
        fish.setWeight(2);
        ctx.getStub().putStringState(fish.getId().toString(), fish.toJSONString());
        return newSuccessResponse();
    }

    /**
     * get receives the value of a key from the ledger
     *
     * @param ctx
     * @param key
     * @return Response with message and payload
     */
    @Transaction
    public Fish get(Context ctx, String key) {

        String value = ctx.getStub().getStringState(key);
        if (value == null || value.isEmpty()) {
            return null;
        }
        Fish fish = Fish.fromJSONString(value);
        return fish;
    }

    /**
     * Rich query using json to read from world state
     *
     * @param ctx
     * @param query
     * @return Response with string payload
     */
    @Transaction
    public String query(Context ctx, String query) {
        String payload = "";

        //key value pair result iterator
        Iterator<KeyValue> iterator = ctx.getStub().getQueryResult(query).iterator();
        if (!iterator.hasNext()) {
            return "[]";
        }
        while (iterator.hasNext()) {
            payload += iterator.next().getStringValue() + ",";
        }
        payload = payload.substring(0, payload.length() - 1);
        payload = "[" + payload + "]";

        return payload;
    }

    /**
     * set stores the asset (both key and value) on the ledger. If the key
     * exists, it will override the value with the new one
     *
     * @param ctx
     * @param key
     * @param value
     * @return value
     */
    @Transaction
    public String set(Context ctx, String key, String value) {

        ctx.getStub().putStringState(key, value);
        return "Succesfully set key : " + key + " as value : " + value;
    }

    /**
     * Delete the key from the state in ledger
     *
     * @param ctx
     * @param key
     * @return Response with message and payload
     */
    @Transaction
    public String delete(Context ctx, String key) {

        // Delete the key from the state in ledger
        ctx.getStub().delState(key);
        return "Succesfully deleted key : " + key + "from the ledger";
    }

    /**
     * getHistory returns all transactions for an object by its key This does
     * not include read only operations (which don't use a transaction!)
     *
     * @param ctx
     * @param key
     * @return Response with message and payload
     */
    @Transaction
    public String getHistory(Context ctx, String key) {
        String payload = "";
        List<Map<String, Object>> historyList = new ArrayList<>();

        //key value pair result iterator
        Iterator<KeyModification> iterator = ctx.getStub().getHistoryForKey(key).iterator();
        if (!iterator.hasNext()) {
            return "[]";
        }
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModules(new JavaTimeModule());
        while (iterator.hasNext()) {
            HashMap<String, Object> history = new HashMap<>();
            KeyModification modification = iterator.next();
            history.put("asset", modification.getStringValue());
            history.put("transactionId", modification.getTxId());
            history.put("timeStamp", modification.getTimestamp());
            historyList.add(history);
        }
        try {
            payload = objectMapper.writeValueAsString(historyList);
        } catch (JsonProcessingException ex) {
            Logger.getLogger(FishChaincode.class.getName()).log(Level.SEVERE, null, ex);
        }

        Response response = newSuccessResponse("Query succesful", payload.getBytes(StandardCharsets.UTF_8));
        return response.getStringPayload();
    }

}