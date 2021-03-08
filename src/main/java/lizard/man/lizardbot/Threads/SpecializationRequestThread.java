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

import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import lizard.man.lizardbot.Bots.LizardBot;
import lizard.man.lizardbot.Interfaces.SpecializationInfoInterface;
import lizard.man.lizardbot.Listeners.EventWaiter;
import lizard.man.lizardbot.Models.Rank;
import lizard.man.lizardbot.Models.Requirement;
import lizard.man.lizardbot.Models.Specialization;
import lizard.man.lizardbot.Services.CensusAPIService;
import lizard.man.lizardbot.repositories.RankRepository;
import lizard.man.lizardbot.repositories.SpecializationsRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.PrivateChannel;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;

public class SpecializationRequestThread implements Runnable {
    // services
    private CensusAPIService cas;
    private SpecializationsRepository sr;
    private RankRepository rr;
    private EventWaiter ew;

    // request storage
    private HashSet<Long> ids;
    private Long charid;
    private String[] requests;
    private HashSet<String> checkedRequests = new HashSet<String>();
    private long maxRequests;
    private String rank = null;
    private long rankLevel;
    private String originalNickname;

    // event storage
    private Message message;
    private MessageChannel channel;
    private PrivateChannel privateChannel;
    private Member member;
    private User author;
    private Guild guild;


    private Runnable threadReference = this;

    private class Bool{ 
        private boolean response = false;
        private boolean timeout = false;
        public void setResponse(boolean response){
            this.response = response;
        }
        public void setTimeout(boolean timeout){
            this.timeout = timeout;
        }
    }

    public SpecializationRequestThread(GuildMessageReceivedEvent event, LizardBot bot){
        this.message = event.getMessage();
        this.channel = event.getChannel();
        this.member = event.getMember();
        this.author = event.getAuthor();
        this.guild = event.getGuild();
        this.privateChannel = author.openPrivateChannel().complete();
        this.sr = bot.getSr();
        this.cas = bot.getCas();
        this.ew = bot.getEw();
        this.rr = bot.getRr();
    }

    public void run() {
        if (processRequest()) {
            if(!(message.getContentRaw().split(" ").length > 2)){

                EmbedBuilder eb = new EmbedBuilder();
                eb.setTitle("Possible Specializations");
                eb.setDescription("inputs for each spec");

                Iterator<SpecializationInfoInterface> infoItr = sr.findRoleAndCommand().iterator();
                while(infoItr.hasNext()){
                    SpecializationInfoInterface info = infoItr.next();
                    eb.addField(info.getRole() + ":", info.getCommand(), true);
                }
                privateChannel.sendMessage(eb.build()).complete();
                privateChannel.sendMessage("Example Response:\nbomb engi liberator").complete();
                privateChannel.sendMessage(author.getAsMention() + " Please state the specializations you would like to request in a space seperated list.\n You can do multiple specs at the same time.").complete();
                
                requests = new String[]{};
                ew.waitForEvent(PrivateMessageReceivedEvent.class, e -> e.getAuthor().equals(author), e -> {
                    requests = e.getMessage().getContentRaw().split("\\s+");
                    synchronized(threadReference){threadReference.notify();}
                }, 60, TimeUnit.SECONDS, new Runnable(){
                    public void run(){
                        privateChannel.sendMessage(author.getAsMention() + " Request timed out").complete();
                        synchronized(threadReference){threadReference.notify();}
                    }
                });
                synchronized(threadReference){
                    try{
                        threadReference.wait();
                    }catch(InterruptedException e){}
                }
            }else{
                requests = message.getContentRaw().split("\\s+");
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
            
            maxRequests = sr.count();

            if(requests.length > maxRequests){
                privateChannel.sendMessage(author.getAsMention() + " Request contains too many entries. Do not spam the bot please").complete();
                return;
            }

            for(String r : requests){
                if(checkedRequests.contains(r)){
                    continue;
                }
                checkedRequests.add(r);
                Specialization spec = sr.findByCommandIgnoreCase(r);
                if(spec != null){
                    if(!spec.getType().equals("b")){
                        if(rankLevel > rr.findLowestRank().getLevel() - 1){
                            privateChannel.sendMessage(author.getAsMention() + " You must be rank " + rr.findByLevel(rr.findLowestRank().getLevel() - 1).getRole() + " or higher to request an advanced spec.").complete();
                            continue;
                        }
                    }
                    checkRequirements(spec);
                }else{
                    privateChannel.sendMessage(author.getAsMention() + " " + r + " is not a valid spec. use <@789243746344632340> request to see all specs").complete();
                }
            }
        }
        privateChannel.close();
    }

    private boolean processRequest(){
        if(!guild.getId().equals("691820171240931339")){
            channel.sendMessage(author.getAsMention() + " The request command is specific to the 2RAF discord").complete();
            return false;
        }

        boolean hasRank = false;
        Rank lowestRank = rr.findLowestRank();
        rankLevel = lowestRank.getLevel() + 1;
        for(Role r : member.getRoles()){
            if(rr.existsByRole(r.getName())){
                long roleLevel = rr.findByRole(r.getName()).getLevel();
                if(roleLevel <= lowestRank.getLevel()){
                    hasRank = true;
                    if(roleLevel < rankLevel){
                        rankLevel = roleLevel;
                    }
                }
            }
        }
        
        if(!hasRank){
            channel.sendMessage(author.getAsMention() + " You must be atleast PFC to use this command. Contact an officer if this is a mistake.").complete();
            return false;
        }
        
        String nickname = member.getEffectiveName();
        
        if(nickname.charAt(0) == '['){
            String[] nicknameArray = null;
            for(int i = 1; i < nickname.length(); i++){
                if(nickname.charAt(i) == ']'){
                    nicknameArray = new String[]{nickname.substring(0, i+1).strip(), nickname.substring(i+1, nickname.length()).strip().split(" ")[0]};
                    break;
                }else if(i == nickname.length() - 1){
                    channel.sendMessage(author.getAsMention() + " Your nickname is invalid. Please change it to either [Rank] InGameName AnythingElse or just InGameName AnythingElse").complete();
                    return false;
                }
            }
            
            if(nicknameArray != null){
                rank = nicknameArray[0].strip();
            }else{
                channel.sendMessage(author.getAsMention() + " Your nickname is invalid. Please change it to either [Rank] InGameName AnythingElse or just InGameName AnythingElse").complete();
                return false;
            }
            
            nickname = nicknameArray[1].strip();
        }
        
        originalNickname = nickname;
        
        String characterFilter = "[^\\p{L}\\p{N}]";
        nickname = nickname.replaceAll(characterFilter, "");

        channel.sendMessage(author.getAsMention() + " Request started in dms").complete();

        try{
            privateChannel.sendMessage("Started request").complete();
        }catch(Exception e){
            channel.sendMessage(author.getAsMention() + " Could not send dm. Make sure you have dms enabled").complete();
            return false;
        }

        charid = cas.getCharacterId(privateChannel, author, nickname);
        if(charid == null){
            return false;
        }

        ids = cas.getIdsByPlayerName(charid, privateChannel, author);
        if(ids == null){
            return false;
        }
        
        return true;
    }

    private boolean checkManualRequirement(String name, String clss){
        
        
        privateChannel.sendMessage(author.getAsMention() + " Do you have " + name + " unlocked for "+ clss + "? yes/no (default no in 15 seconds)").complete();

        Bool response = new Bool();
        ew.waitForEvent(PrivateMessageReceivedEvent.class, e -> e.getAuthor().equals(author) && e.getMessage().getContentRaw().strip().toLowerCase().equals("yes") || e.getMessage().getContentRaw().strip().toLowerCase().equals("no"), e -> {
            if(e.getMessage().getContentRaw().strip().toLowerCase().equals("yes")){
                response.setResponse(true);
            }
            synchronized(threadReference){threadReference.notify();}
        }, 60, TimeUnit.SECONDS, new Runnable(){
            public void run(){
                response.setTimeout(true);
                synchronized(threadReference){threadReference.notify();}
            }
        });
        synchronized(threadReference){
            try{
                threadReference.wait();
            }catch(InterruptedException e){}
        }

        if(response.timeout){
            privateChannel.sendMessage("Request Timed Out").complete();
            return false;
        }

        return response.response;
    }

    
    private void checkRequirements(Specialization spec){

        for(Role r : member.getRoles()){
            if(member.getRoles().contains(guild.getRolesByName(spec.getRole(), false).get(0))){
                privateChannel.sendMessage(author.getAsMention() + " You already have the spec: " + spec.getRole()).complete();
                return;
            }
        }

        HashSet<String> missingReqs = new HashSet<String>();
        List<String> manualReqs = spec.getManualReqs();
        
        if(manualReqs != null){
            for(String s : manualReqs){
                if(!checkManualRequirement(s, spec.getRole())){
                    missingReqs.add(s);
                }
            }
        }


        Iterator<Requirement> reqItr = spec.getReqs().iterator();
        while(reqItr.hasNext()){
            boolean met = false;
            boolean checkDouble = false;
            boolean checkRanks = false;
            long needed = 1;
            long obtained = 0;
            Requirement req = reqItr.next();
            Iterator<Long> reqIDItr = req.getIds().iterator();
            if(reqIDItr.hasNext()){
                long firstEntry = reqIDItr.next();
                
                if(firstEntry == -1){
                    checkDouble = true;
                }else if (firstEntry == -2){
                    if(!(cas.getDirectiveLevel(privateChannel, charid, reqIDItr.next()) >= reqIDItr.next())){
                        missingReqs.add(req.getName());
                    }
                    continue;
                }else if (firstEntry == -3){
                    needed = reqIDItr.next();
                }else if (firstEntry == -4){
                    checkRanks = true;
                }else if (firstEntry == -5){
                    if(!cas.checkOutfitTime(charid, reqIDItr.next())){
                        missingReqs.add(req.getName());
                    }
                    continue;
                }else if (firstEntry == -6){
                    if(!cas.checkKills(charid, reqIDItr.next())){
                        missingReqs.add(req.getName());
                    }
                    continue;
                }
                if(ids.contains(firstEntry)){
                        met = true;
                }
            }
            
            while(reqIDItr.hasNext()){
                if(checkDouble){
                    if(ids.contains(reqIDItr.next()) && ids.contains(reqIDItr.next())){
                        met = true;
                        break;
                    }
                }else if(checkRanks){
                    long id = reqIDItr.next();
                    String role = sr.findRoleAndCommandBySpecid(id).getRole();
                    Role r = guild.getRolesByName(role, false).get(0);
                    if(member.getRoles().contains(r)){
                        met = true;
                        break;
                    }
                }else if(ids.contains(reqIDItr.next())){
                    obtained++;
                    if(obtained >= needed){
                        met = true;
                        break;  
                    }
                }
            }
            if(!met){
                missingReqs.add(req.getName());
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
            msg = msg + "\nContact an officer for manual consideration if this is an error. Some things are not checkable in the planetside api\nThe API takes 1-3 hours to update, so if you just unlocked these things they will not show up.";
            privateChannel.sendMessage(author.getAsMention() + msg).complete();

        }else{

            if(!(spec.getManualReview() == null)){
                String requirements = "";
                for(String s : spec.getManualReview()){
                    requirements += s + "\n";
                }
                privateChannel.sendMessage("This spec requires manual review for some items, an nco will be in touch").complete();
                guild.getTextChannelById("797248485061558277").sendMessage(author.getAsMention() + " requires manual review for the following items for " + spec.getRole() + " due to lack of info in the api:\n" + requirements + "@here").complete();
                return;
            }

            guild.addRoleToMember(member, guild.getRolesByName(spec.getRole(), false).get(0)).complete();
            privateChannel.sendMessage("Congratulations, you have met the requirements for " + spec.getRole()).complete();
            channel.sendMessage(author.getAsMention() + " Congratulations on your achievement of the " + spec.getRole() + ". You have met the requirements!").complete();
            
            Rank previousRank = rr.findLowestRank();

            if(spec.getType().equals("s")){

                reqItr = spec.getReqs().iterator();
                while(reqItr.hasNext()){
                    Requirement tempReq = reqItr.next();
                    if(tempReq.getIds().get(0) == -4){
                        guild.removeRoleFromMember(member, guild.getRolesByName(sr.findRoleAndCommandBySpecid(tempReq.getIds().get(1)).getRole(), false).get(0)).complete();
                    }
                }
                
                if(rankLevel <= previousRank.getLevel() - 2){
                    return;
                }

                File f = new File("src/main/resources/Outfit_Resource_rules.png");
                privateChannel.sendFile(f, "Outfit_Resource_rules.png").complete();
                privateChannel.sendMessage("Do you agree to the rules above for promotion to " + rr.findByLevel(previousRank.getLevel() - 2).getRole() + "?").complete();
                Bool response = new Bool();
                ew.waitForEvent(PrivateMessageReceivedEvent.class, e -> e.getAuthor().equals(author) && e.getMessage().getContentRaw().strip().toLowerCase().equals("yes") || e.getMessage().getContentRaw().strip().toLowerCase().equals("no"), e -> {
                    if(e.getMessage().getContentRaw().strip().toLowerCase().equals("yes")){
                        response.setResponse(true);
                    }
                    synchronized(threadReference){threadReference.notify();}
                }, 60, TimeUnit.SECONDS, new Runnable(){
                    public void run(){
                        response.setTimeout(true);
                        synchronized(threadReference){threadReference.notify();}
                    }
                });
                synchronized(threadReference){
                    try{
                        threadReference.wait();
                    }catch(InterruptedException e){}
                }

                if(response.timeout){
                    privateChannel.sendMessage("Request Timed Out").complete();
                    return;
                }

                if(!response.response){
                    privateChannel.sendMessage("You must agree to the above rules to recieve the promotion to " + rr.findByLevel(previousRank.getLevel() - 2).getRole()).complete();
                    return;
                }

                previousRank = rr.findByLevel(previousRank.getLevel() - 1);
            }

            Rank nextRank = rr.findByLevel(previousRank.getLevel() - 1);
            if((rank == null || rank.strip().toLowerCase().equals(previousRank.getNametag().toLowerCase().strip())) && member.getRoles().contains(guild.getRolesByName(previousRank.getRole(), false).get(0))){
                try{
                    
                    guild.addRoleToMember(member, guild.getRolesByName(nextRank.getRole(), false).get(0)).complete();
                    if(member.getRoles().contains(guild.getRolesByName(previousRank.getRole(), false).get(0))){
                        guild.removeRoleFromMember(member, guild.getRolesByName(previousRank.getRole(), false).get(0)).complete(); 
                    }
                    member.modifyNickname(nextRank.getNametag() + " " + originalNickname).complete();
                    guild.getTextChannelById("692285236263780352").sendMessage(author.getAsMention() + " Congratulations on your promotion to " + nextRank.getRole()).complete();
                    guild.getTextChannelById("797248485061558277").sendMessage(author.getAsMention() + " requires promotion in game to " + nextRank.getRole()).complete();


                }catch(Exception e){
                    channel.sendMessage(author.getAsMention() + " There was an error processing your promotion to " + nextRank.getRole() + "(possibly with my permissions)\n Contact an officer for manual promotion").complete();
                }
            }
        }
    }
}
