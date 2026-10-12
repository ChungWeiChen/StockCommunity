package com.stockcommunity.postLikes;

import java.time.LocalDateTime;

import com.stockcommunity.posts.Post;
import com.stockcommunity.users.Users;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(uniqueConstraints =
        @UniqueConstraint(columnNames = {"user_id", "post_id"}))

@Getter
@Setter
@NoArgsConstructor
public class PostLike {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long likeId;
	
	@JoinColumn(name="user_id", nullable = false)
	@ManyToOne(fetch = FetchType.LAZY)
	private Users user;
	
	@JoinColumn(name="post_id", nullable = false)
	@ManyToOne(fetch = FetchType.LAZY)
	private Post post;
	
	private LocalDateTime createdAt;
	
	@PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
