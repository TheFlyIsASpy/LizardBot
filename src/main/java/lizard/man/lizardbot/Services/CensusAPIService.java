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
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

@Service
@Data
public class CensusAPIService {

    @Value("http://census.daybreakgames.com/s:" + "${census.service.id}" + "/get/ps2:v2/")
    private String baseURI;

    public HashSet<Long> getIdsByPlayerName(String name, MessageReceivedEvent event){

        CloseableHttpClient client = HttpClients.createDefault();
        HttpPost post = new HttpPost(baseURI + "character/?name.first_lower=" + name.toLowerCase() + "&c:resolve=item&c:show=character_id");

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
                    event.getChannel().sendMessage(event.getAuthor().getAsMention() + " Player does not exsist in the planetside 2 database.\n Make sure your nickname is either [Rank] InGameName AnythingElse or just InGameName AnythingElse").queue();
                    return null;
                }
            }else if(je.has("error")){
                event.getChannel().sendMessage("There was an error in the planetside API: " + je.get("error").toString());
                return null;
            }else{
                event.getChannel().sendMessage("There was an error in the planetside API:");
                return null;
            }
        }catch(Exception e){
            System.out.println(e);
            event.getChannel().sendMessage("There was an error in the planetside API: " + e);
            return null;
        }
    }

    public Long getDirectiveLevel(MessageReceivedEvent event, long charId, long directiveId){
        try{
            CloseableHttpClient client = HttpClients.createDefault();
            HttpPost post = new HttpPost(baseURI + "characters_directive_tree?character_id=" + charId + "&directive_tree_id=" + directiveId + "&c:show=current_level");
            JsonReader jr = new JsonReader(new InputStreamReader(client.execute(post).getEntity().getContent()));
            JsonObject je = JsonParser.parseReader(jr).getAsJsonObject();
            if(je.has("error")){
                event.getChannel().sendMessage("There was an error in the planetside API: " + je.get("error").toString());
                return null;
            }
            return je.get("current_level").getAsLong();
        }catch(Exception e){
            event.getChannel().sendMessage("There was an error in the planetside API: " + e.toString());
            return null;
        }
    }
}
