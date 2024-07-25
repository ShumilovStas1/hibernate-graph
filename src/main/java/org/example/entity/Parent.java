package org.example.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;

import java.util.Set;

@Entity
public class Parent {
    @Id
    @SequenceGenerator(name = "parent_id_seq", sequenceName = "parent_id_seq", allocationSize = 1)
    @GeneratedValue(generator = "parent_id_seq")
    private Long id;
    private String name;
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
    private Set<Child> children;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<Child> getChildren() {
        return children;
    }

    public void setChildren(Set<Child> children) {
        this.children = children;
    }
}
