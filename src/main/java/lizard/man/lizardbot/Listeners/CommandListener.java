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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lizard.man.lizardbot.Threads.BirthdayThread;
import lizard.man.lizardbot.Threads.PromoteThread;
import lizard.man.lizardbot.Threads.SpecializationRequestThread;
import lizard.man.lizardbot.Bots.LizardBot;

import lombok.NoArgsConstructor;

import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

@Component
@NoArgsConstructor
public class CommandListener extends ListenerAdapter {

    @Autowired
    private LizardBot bot;
    
    private ExecutorService es = Executors.newCachedThreadPool();
    

    @Override
    public void onReady(ReadyEvent event){
        System.out.println("*Flicks Tongue*");
    }

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent event){
        String[] message = event.getMessage().getContentRaw().split("\\s+");
        if(message[0].contains("789243746344632340")){
            message[0] = "789243746344632340";
        }
        switch(message[0]){
            case "789243746344632340":
                if(message.length > 1){
                    switch(message[1].toLowerCase()){
                        
                        case "request":
                            if(!event.getChannel().getId().equals("705629800357691473")){
                                return;
                            }
                            bot.execute(new SpecializationRequestThread(event, bot));
                            break;
                        case "promote":
                            bot.execute(new PromoteThread(event, bot));
                            break;
                        case "birthday":
                            bot.execute(new BirthdayThread(event, bot));
                            break;
                        case "help":
                            event.getChannel().sendMessage("I am still developing the bot so I'll do this later").queue();
                            break;
                        default:
                            event.getMessage().getChannel().sendMessage(event.getAuthor().getAsMention() + " " + message[1] + " is not a valid command.").queue();
                            break;
                        
                    }
                }else{
                    event.getMessage().getChannel().sendMessage(event.getAuthor().getAsMention() + "To see a list of commands use: <@789243746344632340> help (actually dont I havent done that command yet)").queue();
                    
                }
                break;
        }
    }
}