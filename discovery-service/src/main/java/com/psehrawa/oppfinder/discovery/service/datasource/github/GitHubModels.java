package com.psehrawa.oppfinder.discovery.service.datasource.github;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;

/**
 * GitHub API response models
 */

@JsonIgnoreProperties(ignoreUnknown = true)
record GitHubSearchResponse(
    @JsonProperty("total_count") int totalCount,
    @JsonProperty("incomplete_results") boolean incompleteResults,
    List<GitHubRepository> items
) {}

@JsonIgnoreProperties(ignoreUnknown = true)
record GitHubRepository(
    Long id,
    String name,
    @JsonProperty("full_name") String fullName,
    GitHubOwner owner,
    @JsonProperty("private") boolean isPrivate,
    @JsonProperty("html_url") String htmlUrl,
    String description,
    boolean fork,
    @JsonProperty("created_at") LocalDateTime createdAt,
    @JsonProperty("updated_at") LocalDateTime updatedAt,
    @JsonProperty("pushed_at") LocalDateTime pushedAt,
    @JsonProperty("git_url") String gitUrl,
    @JsonProperty("ssh_url") String sshUrl,
    @JsonProperty("clone_url") String cloneUrl,
    @JsonProperty("svn_url") String svnUrl,
    String homepage,
    Integer size,
    @JsonProperty("stargazers_count") Integer stargazersCount,
    @JsonProperty("watchers_count") Integer watchersCount,
    String language,
    @JsonProperty("has_issues") boolean hasIssues,
    @JsonProperty("has_projects") boolean hasProjects,
    @JsonProperty("has_wiki") boolean hasWiki,
    @JsonProperty("has_pages") boolean hasPages,
    @JsonProperty("has_downloads") boolean hasDownloads,
    boolean archived,
    boolean disabled,
    @JsonProperty("open_issues_count") Integer openIssuesCount,
    @JsonProperty("forks_count") Integer forksCount,
    @JsonProperty("default_branch") String defaultBranch,
    Double score,
    List<String> topics
) {}

@JsonIgnoreProperties(ignoreUnknown = true)
record GitHubOwner(
    String login,
    Long id,
    @JsonProperty("avatar_url") String avatarUrl,
    @JsonProperty("html_url") String htmlUrl,
    String type
) {}

@JsonIgnoreProperties(ignoreUnknown = true)
record GitHubRelease(
    @JsonProperty("tag_name") String tagName,
    String name,
    boolean draft,
    boolean prerelease,
    @JsonProperty("created_at") LocalDateTime createdAt,
    @JsonProperty("published_at") LocalDateTime publishedAt,
    String body,
    GitHubOwner author
) {}

@JsonIgnoreProperties(ignoreUnknown = true)
record GitHubIssue(
    Long id,
    String title,
    String body,
    String state,
    @JsonProperty("created_at") LocalDateTime createdAt,
    @JsonProperty("updated_at") LocalDateTime updatedAt,
    GitHubOwner user,
    List<GitHubLabel> labels
) {}

@JsonIgnoreProperties(ignoreUnknown = true)
record GitHubLabel(
    String name,
    String color,
    String description
) {}

@JsonIgnoreProperties(ignoreUnknown = true)
record GitHubCommit(
    String sha,
    GitHubCommitDetails commit,
    GitHubOwner author,
    GitHubOwner committer
) {}

@JsonIgnoreProperties(ignoreUnknown = true)
record GitHubCommitDetails(
    String message,
    GitHubCommitAuthor author,
    GitHubCommitAuthor committer
) {}

@JsonIgnoreProperties(ignoreUnknown = true)
record GitHubCommitAuthor(
    String name,
    String email,
    LocalDateTime date
) {}