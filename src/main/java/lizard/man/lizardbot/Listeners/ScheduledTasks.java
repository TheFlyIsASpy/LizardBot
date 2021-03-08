
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

import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Iterator;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lizard.man.lizardbot.Bots.LizardBot;
import lizard.man.lizardbot.Models.Birthday;
import lizard.man.lizardbot.repositories.BirthdayRepository;
import lombok.NoArgsConstructor;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@NoArgsConstructor
@Component
@EnableScheduling
public class ScheduledTasks {
    @Autowired
    private LizardBot bot;

    private BirthdayRepository br;

    private Guild guild;

    private HashSet<String> birthdayCache = new HashSet<String>();

    @Scheduled(cron = "0 0 8 * * *")
    private void checkBirthdays(){
        ZonedDateTime now = LocalDateTime.now().atZone(ZoneId.of("America/Indiana/Indianapolis"));
        DateTimeFormatter format = DateTimeFormatter.ofPattern("MM/dd");
        String date = format.format(now);
        HashSet<Birthday> birthdays = br.findByDate(date);
        Iterator<Birthday> itr = birthdays.iterator();
        if(itr.hasNext()){
            Iterator<Guild> guildItr = bot.getJda().getGuilds().iterator();
            while(guildItr.hasNext()){
                Guild g = guildItr.next();
                if(g.getId().equals("691820171240931339")){
                    guild = g;
                }
            }
        }
        while(itr.hasNext()){
            Birthday b = itr.next();
            Member m = guild.getMemberById(b.getDiscordID());
            try{
                guild.addRoleToMember(m, guild.getRoleById("808010736437166097")).complete();
            }catch(Exception e){
                guild.getTextChannelById("692293569716944906").sendMessage("could not add role to " + m.getAsMention() + " " + e.toString()).complete();
            }
            if(m.getRoles().contains(guild.getRoleById("742376474627276871"))){
                guild.getTextChannelById("742377843522142339").sendMessage("@here Hello everyone! It is " + m.getAsMention() + "'(s) BIRTHDAY! *flicks tongue* HAPPY BIRTHsDAY").complete();
            }
            if(m.getRoles().contains(guild.getRoleById("742065879742808105"))){
                guild.getTextChannelById("742067196674572339").sendMessage("@here Hello everyone! It is " + m.getAsMention() + "'(s) BIRTHDAY! *flicks tongue* HAPPY BIRTHsDAY").complete();
            }
            if(m.getRoles().contains(guild.getRoleById("742065880988647456"))){
                guild.getTextChannelById("742067726893318195").sendMessage("@here Hello everyone! It is " + m.getAsMention() + "'(s) BIRTHDAY! *flicks tongue* HAPPY BIRTHsDAY").complete();
            }
            guild.getTextChannelById("772135443612696607").sendMessage("@here Hello everyone! It is " + m.getAsMention() + "'(s) BIRTHDAY! *flicks tongue* HAPPY BIRTHsDAY").complete();
            birthdayCache.add(b.getDiscordID());
        }
    }

    @Scheduled(cron = "0 0 7 * * *")
    private void removeBirthdays(){
        if(birthdayCache.size() > 0){
            Iterator<String> itr = birthdayCache.iterator();
            while(itr.hasNext()){
                String id = itr.next();
                guild.removeRoleFromMember(guild.getMemberById(id), guild.getRoleById("808010736437166097")).complete();
            }
            birthdayCache = new HashSet<String>();
        }
    }

    @PostConstruct
    private void setup(){
        br = bot.getBr();
    }
}