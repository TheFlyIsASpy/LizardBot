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
package lizard.man.lizardbot.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import lizard.man.lizardbot.Interfaces.SpecializationInfoInterface;
import lizard.man.lizardbot.Models.Specialization;

@Repository
public interface SpecializationsRepository extends CrudRepository<Specialization, Long>{

    Specialization findByCommandIgnoreCase(String command);

    @Query("SELECT s.role as role, s.command as command from Specialization s order by s.order asc")
    List<SpecializationInfoInterface> findRoleAndCommand();

    SpecializationInfoInterface findRoleAndCommandBySpecid(long specid);
    
}
