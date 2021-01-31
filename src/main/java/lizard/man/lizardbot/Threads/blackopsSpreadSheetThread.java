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

import java.util.List;

import lizard.man.lizardbot.Bots.LizardBot;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;

public class blackopsSpreadSheetThread implements Runnable {

    private Guild guild;
    private List<Member> members;
    
    public blackopsSpreadSheetThread(LizardBot bot) {
        guild = bot.getJda().getGuildById("691820171240931339");
        members = guild.getMembersWithRoles(guild.getRolesByName("⚔ BLACK OPS", false));
    }
    
    public void run(){
        
    }
}

