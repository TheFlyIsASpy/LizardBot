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

import lizard.man.lizardbot.Models.Rank;
import lizard.man.lizardbot.repositories.RankRepository;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.HierarchyException;
import net.dv8tion.jda.api.entities.Member;

public class PromoteThread implements Runnable{
    
    private MessageReceivedEvent event;
    private RankRepository rr;

    private Member member;
    private Rank lowestRank;

    public PromoteThread(MessageReceivedEvent event, RankRepository rr){
        this.event = event;
        this.rr = rr;
    }
    
    public void run(){
        if(processRequest()){
            try{
                event.getGuild().addRoleToMember(member, event.getGuild().getRolesByName(lowestRank.getRole(), false).get(0)).queue();
                member.modifyNickname(lowestRank.getNametag() + " " + member.getEffectiveName()).queue();
                event.getChannel().sendMessage(member.getAsMention() + " Congratulations on your promotion to PFC!").queue();
            }catch(HierarchyException e){
                event.getChannel().sendMessage(event.getAuthor().getAsMention() + " The recipient has too high permissions for me to edit them").queue();
            }
        }
    }

    private boolean processRequest(){
        if(!event.getGuild().getId().equals("691820171240931339")){
            event.getChannel().sendMessage(event.getAuthor().getAsMention() + " The promote command is specific to the 2RAF discord").queue();
            //return false;
        }
        if(!(event.getMember().getRoles().contains(event.getGuild().getRolesByName("Mentor", false).get(0)))){
            event.getChannel().sendMessage(event.getAuthor().getAsMention() + " You must be a mentor to use this command. If you are an officer, stop hogging the bot").queue();
            return false;
        }

        String[] request = event.getMessage().getContentRaw().split(" ");
        if(!(request.length > 2)){
            event.getChannel().sendMessage(event.getAuthor().getAsMention() + " Usage: <@789243746344632340> promote @recipient").queue();
            return false;
        }

        String recipient = request[2];

        if(request[2].substring(0, 1).equals("<")){
            String characterFilter = "[^\\p{L}\\p{N}]";
            recipient = request[2].replaceAll(characterFilter, "");
        }else{
            event.getChannel().sendMessage(event.getAuthor().getAsMention() + " Usage: <@789243746344632340> promote @recipient").queue();
            return false;
        }

        member = event.getGuild().getMemberById(recipient);

        if(member == null){
            event.getChannel().sendMessage(event.getAuthor().getAsMention() + " " + request[2] + " does not exist or is not in the server").queue();
            return false;
        }
        
        lowestRank = rr.findLowestRank();
        for(Role r : member.getRoles()){
            if(rr.existsByRole(r.getName())){
                if(rr.findByRole(r.getName()).getLevel() <= lowestRank.getLevel()){
                    event.getChannel().sendMessage(event.getAuthor().getAsMention() + " Recipient is already pfc or higher").queue();
                    return false;
                }
            }
        }
        return true;
    }
}
