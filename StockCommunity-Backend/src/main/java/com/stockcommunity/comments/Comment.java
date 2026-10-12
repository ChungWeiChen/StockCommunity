package com.stockcommunity.comments;

import java.time.LocalDateTime;

import com.stockcommunity.posts.Post;
import com.stockcommunity.users.Users;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "comments")
@Getter
@Setter
@NoArgsConstructor
public class Comment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	private String commentText;
	
	@Lob
	private byte[] commentPhoto;
	
	private LocalDateTime createdAt;
	
	private LocalDateTime updatedAt;
	
	
	@JoinColumn(name="user_id")
	@ManyToOne(fetch = FetchType.LAZY)
	private Users user;
	
	@JoinColumn(name="post_id")
	@ManyToOne(fetch = FetchType.LAZY)
	private Post post;
	
	
	@PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
	
	@PreUpdate
	protected void onUpdate() {
		this.updatedAt = LocalDateTime.now();
	}
}
