package com.example.youtubebot.oauth;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OAuthConnectionRepository extends JpaRepository<OAuthConnection, Short> {
}
