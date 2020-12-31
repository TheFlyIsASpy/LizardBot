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
package lizard.man.lizardbot.Listeners;

import javax.persistence.EntityManager;

import java.util.concurrent.ExecutorService; 
import java.util.concurrent.Executors; 

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lizard.man.lizardbot.Threads.SpecializationRequestThread;
import lizard.man.lizardbot.Services.CensusAPIService;
import lombok.NoArgsConstructor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;


@Component
@NoArgsConstructor
public class CommandListener extends ListenerAdapter{
    
    @Autowired
    private CensusAPIService cas;
    @Autowired
    private EntityManager entityManager;

    @Autowired 
    private EventWaiter ew;

    private ExecutorService es = Executors.newCachedThreadPool();

    @Override
    public void onReady(ReadyEvent event){
        System.out.println("*Flicks Tongue*");
    }
    
    @Override
    public void onMessageReceived(MessageReceivedEvent event){
        String[] message = event.getMessage().getContentRaw().split(" ");
        switch(message[0]){
            case "<@789243746344632340>":
            case "<@!789243746344632340>":
                if(message.length > 1){
                    switch(message[1].toLowerCase()){
                        case "request":
                            es.execute(new SpecializationRequestThread(event, cas, ew, entityManager));
                            break;
                        case "help":
                            EmbedBuilder eb = new EmbedBuilder();
                            eb.setTitle("Lizard Bot Commands");
                            eb.setDescription("A list of <@789243746344632340> commands");
                            eb.addField("Usage", "<@789243746344632340> command", false);
                            eb.addField("request:", "Starts a specialization request, only usable in 2RAF", false);
                            eb.addField("help:", "Displays a list of commands", false);
                            event.getChannel().sendMessage(event.getAuthor().getAsMention()).queue();
                            event.getChannel().sendMessage(eb.build()).queue();
                            break;
                        default:
                            event.getMessage().getChannel().sendMessage(event.getAuthor().getAsMention() + " " + message[1] + " is not a valid command.").queue();
                            break;
                    }
                }else{
                    event.getMessage().getChannel().sendMessage(event.getAuthor().getAsMention() + "To see a list of commands use: <@789243746344632340> help").queue();
                }
                break;
        }
    }
}