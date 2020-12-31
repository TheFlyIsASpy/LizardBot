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
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import javax.persistence.EntityManager;

import org.springframework.data.jpa.repository.support.SimpleJpaRepository;

import lizard.man.lizardbot.Listeners.EventWaiter;
import lizard.man.lizardbot.Models.AircraftMechanicRequirement;
import lizard.man.lizardbot.Models.BombardierRequirement;
import lizard.man.lizardbot.Models.EngineerRequirement;
import lizard.man.lizardbot.Models.FlightSurgeonRequirement;
import lizard.man.lizardbot.Models.HeavyAssaultRequirement;
import lizard.man.lizardbot.Models.InfiltratorRequirement;
import lizard.man.lizardbot.Models.LightAssaultRequirement;
import lizard.man.lizardbot.Models.MaxRequirement;
import lizard.man.lizardbot.Models.MedicRequirement;
import lizard.man.lizardbot.Models.Requirement;
import lizard.man.lizardbot.Models.TankerRequirement;
import lizard.man.lizardbot.Services.CensusAPIService;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.HierarchyException;

public class SpecializationRequestThread implements Runnable {
    private CensusAPIService cas;
    private EntityManager entityManager;
    private EventWaiter ew;

    private MessageReceivedEvent event;
    private HashSet<Long> ids;
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
            EventWaiter ew, EntityManager entityManager) {
        this.event = event;
        this.entityManager = entityManager;
        this.cas = cas;
        this.ew = ew;
    }

    public void run() {
        if (processRequest()) {
            EmbedBuilder eb = new EmbedBuilder();
            eb.setTitle("Possible Specializations");
            eb.setDescription("inputs for each spec");
            eb.addField("Infiltrator:", "infil", true);
            eb.addField("Light Assault:", "la", true);
            eb.addField("Medic:", "medic", true);
            eb.addField("Engineer:", "engi", true);
            eb.addField("Heavy Assault:", "heavy", true);
            eb.addField("Max:", "max", true);
            eb.addField("Flight Surgeon:", "fs", true);
            eb.addField("Bombardier:", "bomb", true);
            eb.addField("Aircraft Mechanic:", "am", true);
            eb.addField("Tanker:", "tanker", true);
            eb.addField("Example Response: ", "la fs bomb engi", false);
            event.getChannel().sendMessage(eb.build()).queue();
            event.getChannel().sendMessage(event.getAuthor().getAsMention() + " Please state the specializations you would like to request in a space seperated list").queue();
            
            requests = new String[]{};
            ew.waitForEvent(MessageReceivedEvent.class, e -> e.getAuthor().equals(event.getAuthor()), e -> {
                requests = e.getMessage().getContentRaw().split(" ");
                synchronized(threadReference){threadReference.notify();}
            }, 15, TimeUnit.SECONDS, new Runnable(){
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
            
            for (int i = 0; i < requests.length; i++) {
                switch (requests[i]) {
                    case "infil":
                        checkRequirements(ids, "🗡️ Infiltration Specialist", InfiltratorRequirement.class, null);
                        break;
                    case "la":
                        checkRequirements(ids, "🚀 Light Assault Specialist", LightAssaultRequirement.class,
                                new String[] { "C-4 x2" });
                        break;
                    case "medic":
                        checkRequirements(ids, "💉 Medical Specialist", MedicRequirement.class,
                                new String[] { "C-4 x2", "NS-66 Punisher + Adaptive Underbarrel" });
                        break;
                    case "engi":
                        checkRequirements(ids, "🔧	Engineering Specialist", EngineerRequirement.class,
                                new String[] { "C-4 x2", "Anti-infantry Mana Turret 5" });
                        break;
                    case "heavy":
                        checkRequirements(ids, "🔫	Heavy Assault Specialist", HeavyAssaultRequirement.class,
                                new String[] { "C-4 x2" });
                        break;
                    case "max":
                        checkRequirements(ids, "📛 MAX Specialist", MaxRequirement.class, null);
                        break;
                    case "fs":
                        checkRequirements(ids, "⚕️ Flight Surgeon", FlightSurgeonRequirement.class, 
                                new String[] {"Triage 5", "C-4 x2", "NS-66 Punisher + Adaptive Underbarrel"});
                        break;
                    case "bomb":
                        checkRequirements(ids, "💥 Bombardier", BombardierRequirement.class, 
                                new String[] { "C-4 x2" });
                        break;
                    case "am":
                        checkRequirements(ids, "🧰 Aircraft Mechanic", AircraftMechanicRequirement.class, null);
                        break;
                    case "tanker":
                        checkRequirements(ids, "🛠 Tanker", TankerRequirement.class, 
                                new String[] { "NS-66 Punisher + Adaptive Underbarrel", "Spitfire Cooldown 3"});
                        break;
                    default:
                        event.getChannel().sendMessage(event.getAuthor().getAsMention() + " " + requests[i] + " is not a valid spec.").queue();
                }
            }
        }
    }

    private boolean processRequest(){
        if(!event.getGuild().getId().equals("691820171240931339")){
            event.getChannel().sendMessage(event.getAuthor().getAsMention() + " The request command is specific to the 2RAF discord").queue();
            return false;
        }
        
        HashSet<String> roles = new HashSet<String>(){};
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
        }, 15, TimeUnit.SECONDS, new Runnable(){
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

        ids = cas.getIdsByPlayerName(nickname, event);
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
        }, 15, TimeUnit.SECONDS, new Runnable(){
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

    
    private <T extends Requirement> void checkRequirements(HashSet<Long> ids, String clss, Class<T> type, String[] manualReqs){
        SimpleJpaRepository<T, Long> repo = new SimpleJpaRepository<T, Long>(type, entityManager);
        
        HashSet<String> missingReqs = new HashSet<String>();
        
        if(manualReqs != null){
            for(String s : manualReqs){
                if(!checkManualRequirement(s, clss)){
                    missingReqs.add(s);
                }
            }
        }
        
        Iterator<? extends Requirement> reqItr = repo.findAll().iterator();
        while(reqItr.hasNext()){
            boolean met = false;
            boolean checkDouble = false;
            Requirement req = reqItr.next();
            
            Iterator<Long> reqIDItr = req.getIds().iterator();
            if(reqIDItr.hasNext()){
                long firstEntry = reqIDItr.next();
                
                if(firstEntry == -1){
                    checkDouble = true;
                }else{
                    if(ids.contains(firstEntry)){
                        met = true;
                    }
                }
            }
            
            while(reqIDItr.hasNext()){
                if(checkDouble){
                    if(ids.contains(reqIDItr.next()) && ids.contains(reqIDItr.next())){
                        met = true;
                        break;
                    }
                }else if(ids.contains(reqIDItr.next())){
                    met = true;
                    break;
                }
            }
            
            if(!met){
                missingReqs.add(req.getName());
            }
        }
        
        if(missingReqs.size() > 0){
            String msg = " You are missing these requirements for " + clss + ": \n";
            
            Iterator<String> itr = missingReqs.iterator();
            while(itr.hasNext()){
                msg = msg + itr.next() + ", ";
            }
            
            msg.strip();
            msg = msg.replaceAll(", $", "");
            msg = msg + ".\nContact an officer for manual consideration if this is an error. Some things are not checkable in the planetside api";
            event.getChannel().sendMessage(event.getAuthor().getAsMention() + msg).queue();
        }else{
            event.getChannel().sendMessage(event.getAuthor().getAsMention() + " Congratulations on your achievement of the " + clss + ". You have met the requirements!").queue();
            event.getGuild().addRoleToMember(event.getMember(), event.getGuild().getRolesByName(clss, false).get(0)).queue();
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
