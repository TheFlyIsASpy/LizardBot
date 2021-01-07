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
package lizard.man.lizardbot.Models;

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Table;

import com.vladmihalcea.hibernate.type.array.ListArrayType;

import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name="specializations")
@TypeDef(
    name = "list",
    typeClass = ListArrayType.class
)
@Getter @Setter @NoArgsConstructor
public class Specialization {

    @Id
    @Column(name = "specid")
    private long specid;

    @Column(name = "role")
    private String role;

    @Column(name = "command")
    private String command;

    @Type(type = "list")
    @Column(name = "manual_checks")
    private List<String> manualReqs;

    @Column(name = "order")
    private long order;

    @Column(name = "type")
    private String type;

    @Type(type = "list")
    @Column(name = "manual_review")
    private List<String> manualReview;

    @OneToMany(targetEntity = Requirement.class, mappedBy = "spec", cascade=CascadeType.ALL, fetch = FetchType.EAGER)
    private List<Requirement> reqs;

    public Specialization(long specid, String role, String command, String type){
        this.specid = specid;
        this.role = role;
        this.command = command;
        this.type = type;
    }
}