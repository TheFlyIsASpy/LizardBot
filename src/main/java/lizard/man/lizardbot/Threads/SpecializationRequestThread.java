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

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import lizard.man.lizardbot.Interfaces.SpecializationInfoInterface;
import lizard.man.lizardbot.Listeners.EventWaiter;
import lizard.man.lizardbot.Models.Requirement;
import lizard.man.lizardbot.Models.Specialization;
import lizard.man.lizardbot.Services.CensusAPIService;
import lizard.man.lizardbot.repositories.SpecializationsRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.HierarchyException;

public class SpecializationRequestThread implements Runnable {
    private CensusAPIService cas;
    private SpecializationsRepository sr;
    private EventWaiter ew;

    private MessageReceivedEvent event;
    private HashSet<Long> ids;
    private Long charid;
    private String[] requests;
    private String rank = null;
    private String originalNickname;


    private Runnable threadReference = this;

    private class Bool{ 
        private boolean response = false;
        public void setResponse(boolean response){
            this.response = response;
        }
    }

    public SpecializationRequestThread(MessageReceivedEvent event, CensusAPIService cas,
            EventWaiter ew, SpecializationsRepository sr) {
        this.event = event;
        this.sr = sr;
        this.cas = cas;
        this.ew = ew;
    }

    public void run() {
        if (processRequest()) {
            if(!(event.getMessage().getContentRaw().split(" ").length > 2)){
                EmbedBuilder eb = new EmbedBuilder();
                eb.setTitle("Possible Specializations");
                eb.setDescription("inputs for each spec");
                Iterator<SpecializationInfoInterface> infoItr = sr.findRoleAndCommand().iterator();
                while(infoItr.hasNext()){
                    
                    SpecializationInfoInterface info = infoItr.next();
                    eb.addField(info.getRole(), info.getCommand(), false);
                }
                eb.addField("Example Response: ", "la fs bomb engi liberator", false);
                event.getChannel().sendMessage(eb.build()).queue();
                event.getChannel().sendMessage(event.getAuthor().getAsMention() + " Please state the specializations you would like to request in a space seperated list").queue();
                
                requests = new String[]{};
                ew.waitForEvent(MessageReceivedEvent.class, e -> e.getAuthor().equals(event.getAuthor()), e -> {
                    requests = e.getMessage().getContentRaw().split("\\s+");
                    synchronized(threadReference){threadReference.notify();}
                }, 30, TimeUnit.SECONDS, new Runnable(){
                    public void run(){
                        event.getChannel().sendMessage(event.getAuthor().getAsMention() + " Request timed out").queue();
                        synchronized(threadReference){threadReference.notify();}
                    }
                });
                synchronized(threadReference){
                    try{
                        threadReference.wait();
                    }catch(InterruptedException e){}
                }
            }else{
                requests = event.getMessage().getContentRaw().split("\\s+");
                if(requests.length > 2){
                    String[] temp = new String[requests.length - 2];
                    for(int i = 0; i < requests.length - 2; i++){
                        temp[i] = requests[i+2];
                    }
                    requests = temp;
                }else{
                    requests = new String[]{};
                }

            }
            
            for(String r : requests){
                Specialization spec = sr.findByCommandIgnoreCase(r);
                if(spec != null){
                    checkRequirements(spec);
                }else{
                    event.getChannel().sendMessage(event.getAuthor().getAsMention() + " " + r + " is not a valid spec.").queue();
                }
            }
        }
    }

    private boolean processRequest(){
        if(!event.getGuild().getId().equals("691820171240931339")){
            event.getChannel().sendMessage(event.getAuthor().getAsMention() + " The request command is specific to the 2RAF discord").queue();
            //return false;
        }
        
        HashSet<String> roles = new HashSet<String>();
        roles.add("Private First Class");
        roles.add("Specialist");
        roles.add("Lance Corporal");
        roles.add("Corporal");
        roles.add("Sergeant");
        roles.add("Lieutenant");
        roles.add("Captain");
        
        boolean hasRank = false;
        for(Role r : event.getMember().getRoles()){
            if(roles.contains(r.getName())){
                hasRank = true;
                break;
            }
        }
        
        if(!hasRank){
            event.getChannel().sendMessage(event.getAuthor().getAsMention() + "You must be atleast PFC to use this command. Contact an officer if this is a mistake.").queue();
            return false;
        }
        
        String nickname = event.getMember().getNickname();
        
        if(nickname == null){
            nickname = event.getAuthor().getName();
        }else{
            nickname = nickname.strip();
        }
        
        if(nickname.charAt(0) == '['){
            String[] nicknameArray = null;
            for(int i = 1; i < nickname.length(); i++){
                if(nickname.charAt(i) == ']'){
                    nicknameArray = new String[]{nickname.substring(0, i+1).strip(), nickname.substring(i+1, nickname.length()).strip().split(" ")[0]};
                    break;
                }else if(i == nickname.length() - 1){
                    event.getChannel().sendMessage(event.getAuthor().getAsMention() + "Your nickname is invalid. Please change it to either [Rank] InGameName AnythingElse or just InGameName AnythingElse").queue();
                    return false;
                }
            }
            
            if(nicknameArray != null){
                rank = nicknameArray[0].strip();
            }else{
                event.getChannel().sendMessage(event.getAuthor().getAsMention() + "Your nickname is invalid. Please change it to either [Rank] InGameName AnythingElse or just InGameName AnythingElse").queue();
                return false;
            }
            
            nickname = nicknameArray[1].strip();
        }
        
        originalNickname = nickname;
        
        String characterFilter = "[^\\p{L}\\p{N}]";
        nickname = nickname.replaceAll(characterFilter, "");

        Bool response = new Bool();
        event.getChannel().sendMessage(event.getAuthor().getAsMention() + " Requesting using character: " + nickname + ". Is this your ingame name? yes/no (default no in 15 seconds)").queue();
        ew.waitForEvent(MessageReceivedEvent.class, e -> e.getAuthor().equals(event.getAuthor()) && e.getMessage().getContentRaw().strip().toLowerCase().equals("yes") || e.getMessage().getContentRaw().strip().toLowerCase().equals("no"), e -> {
            if(e.getMessage().getContentRaw().strip().toLowerCase().equals("yes")){
                response.setResponse(true);
            }
            synchronized(threadReference){threadReference.notify();}
        }, 30, TimeUnit.SECONDS, new Runnable(){
            public void run(){
                synchronized(threadReference){threadReference.notify();}
            }
        });
        synchronized(threadReference){
            try{
                threadReference.wait();
            }catch(InterruptedException e){}
        }

        if(!response.response){
            event.getChannel().sendMessage(event.getAuthor().getAsMention() + " You must set your nickname to to either [Rank] InGameName AnythingElse or just InGameName AnythingElse").queue();
            return false;
        }

        charid = cas.getCharacterId(event, nickname);
        if(charid == null){
            return false;
        }
        ids = cas.getIdsByPlayerName(charid, event);
        if(ids == null){
            return false;
        }
        
        return true;
    }

    private boolean checkManualRequirement(String name, String clss){
        Bool hasIt = new Bool();
        
        event.getChannel().sendMessage(event.getAuthor().getAsMention() + " Do you have " + name + " unlocked for "+ clss + "? yes/no (default no in 15 seconds)").queue();
        
        ew.waitForEvent(MessageReceivedEvent.class, e -> e.getAuthor().equals(event.getAuthor()) && e.getMessage().getContentRaw().strip().toLowerCase().equals("yes") || e.getMessage().getContentRaw().strip().toLowerCase().equals("no"), e -> {
            if(e.getMessage().getContentRaw().strip().toLowerCase().equals("yes")){
                hasIt.setResponse(true);
            }
            synchronized(threadReference){threadReference.notify();}
        }, 30, TimeUnit.SECONDS, new Runnable(){
            public void run(){
                synchronized(threadReference){threadReference.notify();}
            }
        });
        synchronized(threadReference){
            try{
                threadReference.wait();
            }catch(InterruptedException e){}
        }
        return hasIt.response;
    }

    
    private void checkRequirements(Specialization spec){


        HashSet<String> missingReqs = new HashSet<String>();
        List<String> manualReqs = spec.getManualReqs();
        
        if(manualReqs != null){
            for(String s : manualReqs){
                if(!checkManualRequirement(s, spec.getRole())){
                    missingReqs.add(s);
                }
            }
        }


        Hashtable<Requirement, Boolean> groupedRequirements = new Hashtable<Requirement, Boolean>();
        Iterator<Requirement> reqItr = spec.getReqs().iterator();
        while(reqItr.hasNext()){
            boolean met = false;
            boolean checkDouble = false;
            long needed = 1;
            long obtained = 0;
            Requirement req = reqItr.next();
            Iterator<Long> reqIDItr = req.getIds().iterator();
            if(reqIDItr.hasNext()){
                long firstEntry = reqIDItr.next();
                
                if(firstEntry == -4){
                    firstEntry = reqIDItr.next();
                    groupedRequirements.put(req, false);
                }
                if(firstEntry == -1){
                    checkDouble = true;
                }else if (firstEntry == -2){
                    if(!(cas.getDirectiveLevel(event, charid, reqIDItr.next()) >= reqIDItr.next())){
                        missingReqs.add(req.getName());
                    }
                    continue;
                }else if (firstEntry == -3){
                    needed = reqIDItr.next();
                }if(ids.contains(firstEntry)){
                        met = true;
                }
            }
            
            while(reqIDItr.hasNext()){
                if(checkDouble){
                    if(ids.contains(reqIDItr.next()) && ids.contains(reqIDItr.next())){
                        met = true;
                        break;
                    }
                }else if(ids.contains(reqIDItr.next())){
                    obtained++;
                    if(obtained >= needed){
                        met = true;
                        if(groupedRequirements.contains(req)){
                            groupedRequirements.put(req, true);
                        }
                        break;  
                    }
                }
            }
            if(!met){
                missingReqs.add(req.getName());
            }
        }

        if(groupedRequirements.size() > 0){
            boolean met = false;
            Iterator<Requirement> itr = groupedRequirements.keySet().iterator();
            String compoundReq = "";
            while(itr.hasNext()){
                Requirement req = itr.next();
                compoundReq = compoundReq + req.getName() + " or ";
                if(groupedRequirements.get(req)){
                    met = true;
                    break;
                }
            }
            compoundReq = compoundReq.replaceAll("or $", "");
            if(!met){
                missingReqs.add(compoundReq);
            }
        }
        
        if(missingReqs.size() > 0){
            String msg = " You are missing these requirements for " + spec.getRole() + ": \n";
            
            Iterator<String> itr = missingReqs.iterator();
            while(itr.hasNext()){
                msg = msg + itr.next() + ",\n";
            }
            
            msg.strip();
            msg = msg.replaceAll("\n$", "");
            msg = msg + "\nContact an officer for manual consideration if this is an error. Some things are not checkable in the planetside api";
            event.getChannel().sendMessage(event.getAuthor().getAsMention() + msg).queue();
        }else{
            event.getChannel().sendMessage(event.getAuthor().getAsMention() + " Congratulations on your achievement of the " + spec.getRole() + ". You have met the requirements!").queue();
            //event.getGuild().addRoleToMember(event.getMember(), event.getGuild().getRolesByName(spec.getRole(), false).get(0)).queue();
            if((rank == null || rank.strip().toLowerCase().equals("[6 pfc]")) && event.getMember().getRoles().contains(event.getGuild().getRolesByName("Private First Class", false).get(0))){
                try{
                    event.getGuild().addRoleToMember(event.getMember(), event.getGuild().getRolesByName("Specialist", false).get(0)).queue();
                    event.getGuild().removeRoleFromMember(event.getMember(), event.getGuild().getRolesByName("Private First Class", false).get(0)).queue();
                    event.getMember().modifyNickname("[5 Spc] " + originalNickname).queue();
                    event.getChannel().sendMessage(event.getAuthor().getAsMention() + " Congratulations on your promotion to specialist!").queue();
                }catch(HierarchyException e){
                    event.getChannel().sendMessage(event.getAuthor().getAsMention() + " There was an error processing your promotion from PFC (possibly with my permissions)\n Contact an officer for manual promotion").queue();
                }
            }
        }
    }
}
