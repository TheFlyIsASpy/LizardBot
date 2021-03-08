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
package lizard.man.lizardbot.Threads;

import java.util.concurrent.TimeUnit;

import lizard.man.lizardbot.Bots.LizardBot;
import lizard.man.lizardbot.Listeners.EventWaiter;
import lizard.man.lizardbot.Models.Birthday;
import lizard.man.lizardbot.repositories.BirthdayRepository;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.PrivateChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;

public class BirthdayThread implements Runnable {

    private User author;
    private PrivateChannel privateChannel;
    private MessageChannel originChannel;
    private EventWaiter ew;

    private BirthdayRepository br;

    private String response = "";

    private Runnable threadReference = this;
    
    public BirthdayThread(GuildMessageReceivedEvent event, LizardBot bot) {
        this.author = event.getAuthor();
        this.originChannel = event.getChannel();
        this.privateChannel = author.openPrivateChannel().complete();
        this.ew = bot.getEw();
        this.br = bot.getBr();
    }
    
    public void run(){

        originChannel.sendMessage("pssssst... check your dms").complete();


        if(br.existsByDiscordID(author.getId())){
            try{
                privateChannel.sendMessage("I already have a birthday for you. It is MM/DD: " + br.findByDiscordID(author.getId()).getDate()).complete();
                privateChannel.sendMessage("Would you like to change it yes/no c to cancel").complete();
            }catch(Exception e){
                originChannel.sendMessage("Could not start direct message, make sure you have them enabled").complete();
                return;
            }
            ew.waitForEvent(PrivateMessageReceivedEvent.class, e -> e.getAuthor().equals(author), e -> {
                response = e.getMessage().getContentRaw().strip();
                synchronized(threadReference){threadReference.notify();}
            }, 60, TimeUnit.SECONDS, new Runnable(){
                public void run(){
                    privateChannel.sendMessage(author.getAsMention() + " Request timed out").complete();
                    response = "c";
                    synchronized(threadReference){threadReference.notify();}
                }
            });
            synchronized(threadReference){
                try{
                    threadReference.wait();
                }catch(InterruptedException e){}
            }
            if(response.toLowerCase().equals("c") || !(response.toLowerCase().equals("yes") || response.toLowerCase().equals("y"))){
                privateChannel.sendMessage("ok goodbye *flicks tongue*").complete();
                return;
            }
        }
        response = "";
        setBirthday();
    }

    private void setBirthday(){
        try{
            privateChannel.sendMessage("Please send me your birthday in the following format MM/DD or type c to cancel").complete();
        }catch(Exception e){
            originChannel.sendMessage("Could not start direct message, make sure you have them enabled").complete();
            return;
        }

        for(int i = 0; i < 3; i++){

            ew.waitForEvent(PrivateMessageReceivedEvent.class, e -> e.getAuthor().equals(author), e -> {
                response = e.getMessage().getContentRaw().strip();
                synchronized(threadReference){threadReference.notify();}
            }, 60, TimeUnit.SECONDS, new Runnable(){
                public void run(){
                    privateChannel.sendMessage(author.getAsMention() + " Request timed out").complete();
                    response = "c";
                    synchronized(threadReference){threadReference.notify();}
                }
            });
            synchronized(threadReference){
                try{
                    threadReference.wait();
                }catch(InterruptedException e){}
            }
    
            if(response.toLowerCase().equals("c")){
                privateChannel.sendMessage("ok goodbye *flicks tongue*").complete();
                return;
            }

            if(response.matches("^(0[1-9]|1[0-2])/(0[1-9]|[1-2][0-9]|3[0-1])$")){
                privateChannel.sendMessage("is " + response + " your correct birthday? type yes or no").complete();
                ew.waitForEvent(PrivateMessageReceivedEvent.class, e -> e.getAuthor().equals(author), e -> {
                    if(e.getMessage().getContentRaw().strip().toLowerCase().equals("yes") || e.getMessage().getContentRaw().strip().toLowerCase().equals("y")){
                    }else{
                        response = "c";
                        privateChannel.sendMessage("request has been canceled").complete();
                    }
                    synchronized(threadReference){threadReference.notify();}
                }, 60, TimeUnit.SECONDS, new Runnable(){
                    public void run(){
                        privateChannel.sendMessage(author.getAsMention() + " Request timed out").complete();
                        response = "c";
                        synchronized(threadReference){threadReference.notify();}
                    }
                });
                synchronized(threadReference){
                    try{
                        threadReference.wait();
                    }catch(InterruptedException e){}
                }
                if(response.equals("c")){
                    break;
                }
                try{
                    br.save(new Birthday(author.getId(), response));
                }catch(org.springframework.dao.DataIntegrityViolationException e){
                    privateChannel.sendMessage("You have already entered your birthday!").complete();
                    break;
                }
                privateChannel.sendMessage("Your birthday has been recorded!").complete();
                break;
            }else{
                privateChannel.sendMessage(response + " is not in the correct format, please retype").complete();
            }
            if(i == 2){
                privateChannel.sendMessage("Maximum tries exceeded.").complete();
            }
        }
    }
}
