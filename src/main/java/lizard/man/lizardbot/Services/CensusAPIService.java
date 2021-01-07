/*   Copyright 2020 Nicolas Sheridan (TheFlyIsASpy)

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package lizard.man.lizardbot.Services;

import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.Data;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;

@Service
@Data
public class CensusAPIService {

    @Value("http://census.daybreakgames.com/s:" + "${census.service.id}" + "/get/ps2:v2/")
    private String baseURI;

    public HashSet<Long> getIdsByPlayerName(long id, MessageChannel channel, User author){

        CloseableHttpClient client = HttpClients.createDefault();
        HttpPost post = new HttpPost(baseURI + "character/?character_id=" + id + "&c:resolve=item&c:show=character_id");

        try{
            JsonReader jr = new JsonReader(new InputStreamReader(client.execute(post).getEntity().getContent()));
            JsonObject je = JsonParser.parseReader(jr).getAsJsonObject();
            
            if(je.has("character_list")){
                JsonArray players = je.getAsJsonArray("character_list");
                if(players.size() > 0){

                    JsonArray items = players.get(0).getAsJsonObject().getAsJsonArray("items");
                    HashSet<Long> ids = new HashSet<Long>();
                    for(int i = 0; i < items.size(); i++){
                        ids.add(items.get(i).getAsJsonObject().get("item_id").getAsLong());                    
                    }
                    return ids;
                }else{
                    channel.sendMessage(author.getAsMention() + " Player does not exsist in the planetside 2 database.\n Make sure your nickname is either [Rank] InGameName AnythingElse or just InGameName AnythingElse").queue();
                    return null;
                }
            }else if(je.has("error")){
                channel.sendMessage("There was an error in the planetside API: " + je.get("error").toString());
                return null;
            }else{
                channel.sendMessage("There was an error in the planetside API:");
                return null;
            }
        }catch(Exception e){
            System.out.println(e);
            channel.sendMessage("There was an error in the planetside API: " + e);
            return null;
        }
    }

    public Long getDirectiveLevel(MessageChannel channel, long charId, long directiveId){
        try{
            CloseableHttpClient client = HttpClients.createDefault();
            HttpPost post = new HttpPost(baseURI + "characters_directive_tree?character_id=" + charId + "&directive_tree_id=" + directiveId + "&c:show=current_directive_tier_id");
            JsonReader jr = new JsonReader(new InputStreamReader(client.execute(post).getEntity().getContent()));
            JsonObject je = JsonParser.parseReader(jr).getAsJsonObject();
            if(je.has("error")){
                channel.sendMessage("There was an error in the planetside API: " + je.get("error").toString()).queue();
                return null;
            }
            return je.getAsJsonArray("characters_directive_tree_list").get(0).getAsJsonObject().get("current_directive_tier_id").getAsLong();
        }catch(Exception e){
            channel.sendMessage("There was an error in the planetside API: " + e.toString()).queue();
            return null;
        }
    }

    public Long getCharacterId(MessageChannel channel, User author, String name){
        CloseableHttpClient client = HttpClients.createDefault();
        HttpPost post = new HttpPost(baseURI + "character/?name.first_lower=" + name.toLowerCase() + "&c:show=character_id");

        try{
            JsonReader jr = new JsonReader(new InputStreamReader(client.execute(post).getEntity().getContent()));
            JsonObject je = JsonParser.parseReader(jr).getAsJsonObject();
            
            if(je.has("character_list")){
                JsonArray players = je.getAsJsonArray("character_list");
                if(players.size() > 0){

                    return players.get(0).getAsJsonObject().get("character_id").getAsLong();
                }else{
                    channel.sendMessage(author.getAsMention() + " Player does not exsist in the planetside 2 database.\n Make sure your nickname is either [Rank] InGameName AnythingElse or just InGameName AnythingElse").queue();
                    return null;
                }
            }else if(je.has("error")){
                channel.sendMessage("There was an error in the planetside API: " + je.get("error").toString()).queue();
                return null;
            }else{
                channel.sendMessage("There was an error in the planetside API:").queue();
                return null;
            }
        }catch(Exception e){
            System.out.println(e);
            channel.sendMessage("There was an error in the planetside API: " + e).queue();
            return null;
        }
    }

    public Boolean checkOutfitTime(long id, long days){
        CloseableHttpClient client = HttpClients.createDefault();
        HttpPost post = new HttpPost(baseURI + "character/?character_id=" + id + "&c:resolve=outfit&c:show=character_id");
        try{
            JsonReader jr = new JsonReader(new InputStreamReader(client.execute(post).getEntity().getContent()));
            JsonObject je = JsonParser.parseReader(jr).getAsJsonObject();
            
            if(je.has("character_list")){
                JsonArray players = je.getAsJsonArray("character_list");
                if(players.size() > 0){
                    String joinDate = players.get(0).getAsJsonObject().get("outfit").getAsJsonObject().get("member_since_date").getAsString();
                    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    Date date = format.parse(joinDate);
                    Date now = new Date();
                    
                    long daysInOutfit = ((now.getTime() - date.getTime()) / (1000 * 60 * 60 * 24)) % 365;
                    if(daysInOutfit >= days){
                        return true;
                    }
                    else return false;
                }else{
                    return null;
                }
            }else if(je.has("error")){
                return null;
            }else{
                return null;
            }
        }catch(Exception e){
            System.out.println(e);
            return null;
        }
    }
}
