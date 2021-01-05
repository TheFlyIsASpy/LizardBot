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

    @Query("SELECT s.role as role, s.command as command from Specialization s")
    List<SpecializationInfoInterface> findRoleAndCommand();
}
