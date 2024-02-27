package com.radiologics.plugins.ldapsync.dtos;

import lombok.*;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@Builder
public class LdapGroupProjectManagement {
    private String label;

    private List<String> projectIds;
}
