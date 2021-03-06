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

import java.util.HashSet;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import lizard.man.lizardbot.Models.Rank;

@Repository
public interface RankRepository extends CrudRepository<Rank, Long>{

    @Query("select r from Rank r where r.level = (select max(level) as level from Rank as r2)")
    Rank findLowestRank();

    HashSet<Rank> findAll();

    Rank findByRole(String role);

    boolean existsByRole(String role);

    Rank findByLevel(long level);
}

