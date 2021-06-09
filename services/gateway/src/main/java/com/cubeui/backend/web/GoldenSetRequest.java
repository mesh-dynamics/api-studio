package com.cubeui.backend.web;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoldenSetRequest {
    private String name;
    private String label;
    private String userId;
    private String codeVersion;
    private String branch;
    private String gitCommitId;
    private List tags;

    public String toString() {
        String encoded = "name=" +  this.name
                +"&label=" + this.label
                +"&userId=" + this.userId
                +"&codeVersion=" + this.codeVersion
                +"&branch=" +this.branch
                +"&gitCommitId=" + this.gitCommitId
                +"&tags=" + this.tags;
        return encoded;
    }
}
