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

import lizard.man.lizardbot.Bots.LizardBot;
import lizard.man.lizardbot.Models.Rank;
import lizard.man.lizardbot.repositories.RankRepository;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.HierarchyException;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;

public class PromoteThread implements Runnable {

    private RankRepository rr;

    private Member eventMember;
    private Member recipient;
    private User author;
    private Guild guild;
    private Message message;
    private MessageChannel channel;
    private Rank lowestRank;

    public PromoteThread(GuildMessageReceivedEvent event, LizardBot bot) {
        this.author = event.getAuthor();
        this.eventMember = event.getMember();
        this.guild = event.getGuild();
        this.message = event.getMessage();
        this.rr = bot.getRr();
        this.channel = event.getChannel();
    }
    
    public void run(){
        if(processRequest()){
            try{
                guild.addRoleToMember(recipient, guild.getRolesByName(lowestRank.getRole(), false).get(0)).complete();
                for(Role r : guild.getRolesByName("Private", true)){
                    guild.removeRoleFromMember(recipient, r).complete(); 
                }
                String name = recipient.getEffectiveName().strip();
                int startIndex = 0;
                if(name.substring(0,1).equals("[")){
                    for(int i = 0; i < name.length(); i++){
                        if(name.substring(i, i+1).equals("]")){
                            startIndex = i+1;
                            break;
                        }
                    }
                    if(startIndex == 0){
                        channel.sendMessage(author.getAsMention() + " Recipient has arbitrary [] please rename them").complete();
                        return;
                    }
                }
                name = name.substring(startIndex);
                recipient.modifyNickname(lowestRank.getNametag() + " " + name).complete();
                channel.sendMessage(recipient.getAsMention() + " has been promoted to PFC!").complete();
            }catch(HierarchyException e){
                channel.sendMessage(author.getAsMention() + " The recipient has too high permissions for me to edit them").complete();
            }
        }
    }

    private boolean processRequest(){
        if(!guild.getId().equals("691820171240931339")){
            channel.sendMessage(author.getAsMention() + " The promote command is specific to the 2RAF discord").complete();
            return false;
        }
        if(!(eventMember.getRoles().contains(guild.getRolesByName("Mentor", false).get(0)))){
            channel.sendMessage(author.getAsMention() + " You must be a mentor to use this command. If you are an officer, stop hogging the bot").complete();
            return false;
        }

        String[] request = message.getContentRaw().split("\\s+");
        if(!(request.length > 2)){
            channel.sendMessage(author.getAsMention() + " Usage: <@789243746344632340> promote @recipient").complete();
            return false;
        }

        String id = request[2];

        if(request[2].substring(0, 1).equals("<")){
            String characterFilter = "[^\\p{L}\\p{N}]";
            id = request[2].replaceAll(characterFilter, "");
        }else{
            channel.sendMessage(author.getAsMention() + " Usage: <@789243746344632340> promote @recipient").complete();
            return false;
        }

        recipient = guild.getMemberById(id);

        if(recipient == null){
            channel.sendMessage(author.getAsMention() + " " + request[2] + " does not exist or is not in the server").complete();
            return false;
        }
        
        lowestRank = rr.findLowestRank();
        for(Role r : recipient.getRoles()){
            if(rr.existsByRole(r.getName())){
                if(rr.findByRole(r.getName()).getLevel() <= lowestRank.getLevel()){
                    channel.sendMessage(author.getAsMention() + " Recipient is already pfc or higher").complete();
                    return false;
                }
            }
        }
        return true;
    }
}
