package com.uacf.message;

import java.util.Calendar;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.mongodb.DBCursor;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

@Path("messageservice")
public class MessageService 
{
	private static int id = 1;
	
	@Path("/chat/{id}")
	@GET
	@Produces("application/json")
	public Response getChat(@PathParam("id") Integer id) throws JSONException 
	{
		if (id == null) {
			return Response.status(400).entity("Please enter an id to search").build();
		}
		MongoClient mongo = new MongoClient("localhost" , 27017 );
		MongoDatabase messages = mongo.getDatabase("messages");
		MongoCollection unexpired = messages.getCollection("unexpired");
		MongoCollection expired = messages.getCollection("expired");
		
		Document message = new Document();
		message.put("_id", id);
		DBCursor search = (DBCursor) unexpired.find(message);
		if (!search.hasNext()) {
			search = (DBCursor) expired.find(message);
		}
		if (!search.hasNext()) {
			return Response.status(400).entity("Could not find a message with that ID").build();
		}
		message = (Document) search.next();
		JSONObject json = new JSONObject();
		json.put("username", message.get("username"));
		json.put("text", message.get("text"));
		json.put("expiration_date", message.get("expiration_date"));
		mongo.close();
		return Response.status(200).entity(json).build();
	}
	
	@Path("/chats/{username}")
	@GET
	@Produces("application/json")
	public Response getChats(@PathParam("username") String username) throws JSONException 
	{
		if (username == null) {
			return Response.status(400).entity("Please enter an id to search").build();
		}
		MongoClient mongo = new MongoClient("localhost" , 27017 );
		MongoDatabase messages = mongo.getDatabase("messages");
		MongoCollection unexpired = messages.getCollection("unexpired");
		MongoCollection expired = messages.getCollection("expired");
		
		Document message = new Document();
		message.put("username", username);
		DBCursor search = (DBCursor) unexpired.find(message);
		JSONArray foundMessages = new JSONArray();
		if (!search.hasNext()) {
			return Response.status(400).entity("Could not find any messages from that username").build();
		}
		while (search.hasNext()) {
			Document foundMessage = (Document) search.next();
			JSONObject json = new JSONObject();
			json.put("id", foundMessage.get("_id"));
			json.put("text", foundMessage.get("text"));
			foundMessages.put(json);
			expired.insertOne(foundMessage);
			unexpired.deleteOne(foundMessage);
		}
		return Response.status(200).entity(foundMessages).build();
	}
	
	@Path("post/{username}/{text}/{timeout}")
	@POST
	@Produces("application/json")
	public Response chat(@PathParam("username") String username, @PathParam("text") String text, @PathParam("timeout") Integer timeout)
	{
		if (username == null) {
			return Response.status(400).entity("Please enter a username").build();
		}
		if (text == null) {
			return Response.status(400).entity("Please enter a text message").build();
		}
		MongoClient mongo = new MongoClient( "localhost" , 27017 );
		MongoDatabase messages = mongo.getDatabase("messages");
		Integer id = generateID();
		Document message = new Document("_id", id);
		message.append("username", username);
		message.append("text", text);
		Calendar date = Calendar.getInstance();
		if (timeout == null) {
			date.add(Calendar.SECOND, 60);
		}
		else {
			date.add(Calendar.SECOND, timeout);
		}
		message.append("expiration_date", date);
		JSONObject json = new JSONObject();
		json.put("id", id);
		MongoCollection unexpired = messages.getCollection("unexpired");
		unexpired.insertOne(message);
		mongo.close();
		return Response.status(200).entity(json).build();
	}
	
	public int generateID ()
	{
		return id++;
	}
}
