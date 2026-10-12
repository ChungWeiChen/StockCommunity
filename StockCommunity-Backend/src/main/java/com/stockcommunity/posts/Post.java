package com.stockcommunity.posts;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import com.stockcommunity.stocks.stock.Stock;
import com.stockcommunity.users.Users;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "posts")
@Getter
@Setter
@NoArgsConstructor
public class Post {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;


	private String postTitle;

	@Column(nullable = false)
	private String postText;

	@Lob
	private byte[] photoFile;
	
	private LocalDateTime createdAt;

	private LocalDateTime updatedAt;

	
	@JoinColumn(name="user_id")
	@ManyToOne(fetch = FetchType.LAZY)
	private Users user;

	
	@ManyToMany
	@JoinTable(
	    name = "post_stocks",
	    joinColumns = @JoinColumn(name = "post_id"),
	    inverseJoinColumns = @JoinColumn(name = "stock_code")
	)
	private Set<Stock> stocks = new HashSet<>();
	

	@PrePersist
	protected void onCreate() {
		this.createdAt = LocalDateTime.now();
	}

	@PreUpdate
	protected void onUpdate() {
		this.updatedAt = LocalDateTime.now();
	}
}