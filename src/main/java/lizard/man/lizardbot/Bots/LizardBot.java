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

package lizard.man.lizardbot.Bots;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.PostConstruct;
import javax.security.auth.login.LoginException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lizard.man.lizardbot.Listeners.CommandListener;
import lizard.man.lizardbot.Listeners.EventWaiter;
import lizard.man.lizardbot.Services.CensusAPIService;
import lizard.man.lizardbot.repositories.BirthdayRepository;
import lizard.man.lizardbot.repositories.RankRepository;
import lizard.man.lizardbot.repositories.SpecializationsRepository;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;

@Component
@Getter @Setter
public class LizardBot{

    private JDA jda;

    @Value("${discord.token}")
    private String token;

    //services
    @Autowired
    private CensusAPIService cas;
    @Autowired
    private SpecializationsRepository sr;
    @Autowired
    private RankRepository rr;
    @Autowired
    private BirthdayRepository br;


    //listeners
    @Autowired
    private CommandListener cl;
    @Autowired 
    private EventWaiter ew;

    //threadpool
    private ExecutorService es = Executors.newCachedThreadPool();


    public LizardBot() throws LoginException{
    }

    @PostConstruct
    private void build() throws LoginException{
        jda = JDABuilder.createDefault(token)
                        .addEventListeners(cl,ew)
                        .setChunkingFilter(ChunkingFilter.ALL) // enable member chunking for all guilds
                        .setMemberCachePolicy(MemberCachePolicy.ALL) // ignored if chunking enabled
                        .enableIntents(GatewayIntent.GUILD_MEMBERS)
                        .build();
        jda.getPresence().setStatus(OnlineStatus.ONLINE);
        jda.getPresence().setActivity(Activity.watching("tasty frogs play planetside"));
    }

    public void execute(Runnable thread){
        es.execute(thread);
    }
}
