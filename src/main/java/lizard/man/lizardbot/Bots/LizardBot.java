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

import javax.annotation.PostConstruct;
import javax.security.auth.login.LoginException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lizard.man.lizardbot.Listeners.CommandListener;
import lizard.man.lizardbot.Listeners.EventWaiter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;

@Component
public class LizardBot{

    private JDA jda;

    @Value("${discord.token}")
    private String token;

    //listeners
    @Autowired
    CommandListener cl;
    @Autowired
    EventWaiter ew;
    
    public LizardBot() throws LoginException{
    }

    @PostConstruct
    private void build() throws LoginException{
        jda = JDABuilder.createDefault(token)
                        .addEventListeners(cl,ew)
                        .build();
        jda.getPresence().setStatus(OnlineStatus.ONLINE);
        jda.getPresence().setActivity(Activity.watching("tasty frogs play planetside"));
    }
}
