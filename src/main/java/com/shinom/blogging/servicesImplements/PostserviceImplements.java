package com.shinom.blogging.servicesImplements;


import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shinom.blogging.dto.PostDto;
import com.shinom.blogging.entities.Category;
import com.shinom.blogging.entities.Post;
import com.shinom.blogging.entities.User;
import com.shinom.blogging.exceptions.ResourceNotFoundException;
import com.shinom.blogging.helper.PostResponse;
import com.shinom.blogging.imageUpload.services.ImageUploader;
import com.shinom.blogging.repositories.CategoryRepo;
import com.shinom.blogging.repositories.PostRepo;
import com.shinom.blogging.repositories.UserRepo;
import com.shinom.blogging.services.FileServices;
import com.shinom.blogging.services.PostService;


@Service
public class PostserviceImplements implements PostService {
	
	@Autowired
	private PostRepo postRepo;
	
	@Autowired
	private UserRepo userRepo;
	
	@Autowired
	private CategoryRepo categoryRepo;
	
	@Autowired
	private ModelMapper modelMapper;
	
	@Autowired
	private ObjectMapper objectMapper;
	
	@Autowired
	private ImageUploader imageUploader;
	
	
	private static ZoneId zone = ZoneId.of("Asia/Kolkata");

	@Override
	public PostDto create(PostDto dto, Integer user_id, Integer category_id) {
		Post post = this.dtoToPost(dto);
		User user = this.userRepo.findById(user_id).orElseThrow(()->new ResourceNotFoundException("User", " Id", user_id));
		Category category = this.categoryRepo.findById(category_id).orElseThrow(()->new ResourceNotFoundException("Category ", "Id " ,category_id));
		post.setCategory(category);
		post.setUser(user);
		LocalDate date = LocalDate.now(zone);
		LocalTime time = LocalTime.now(zone);
		LocalDateTime localDateTime = LocalDateTime.now(zone);
	
		post.setCreationDate(date);
		post.setCreationTime(time);
		post.setLastUpdatedDate(localDateTime);
		
		post.setVisible(true);
		
		Post savedPost = postRepo.save(post);
		return this.postToDto(savedPost);
	}

	@Override
	public PostDto update(PostDto dto, Integer post_id) {
		Post post = postRepo.findById(post_id).orElseThrow(()->new ResourceNotFoundException("Post "," Id ", post_id));
		post.setTitle(dto.getTitle());
		post.setContent(dto.getContent());
		LocalDateTime localDateTime = LocalDateTime.now(zone);
		post.setLastUpdatedDate(localDateTime);
		
		Post updatedPost = this.postRepo.save(post);
		return this.postToDto(updatedPost);
	}
	
	@Override
	public PostDto update(Integer post_id) {
		Post post = postRepo.findById(post_id).orElseThrow(()->new ResourceNotFoundException("Post "," Id ", post_id));
		post.setLikes(post.getLikes()+1);
		Post updatedPost = this.postRepo.save(post);
		return this.postToDto(updatedPost);
	}
	
	@Override
	public PostDto updateLikes(Integer post_id, Integer userId) {
		Post post = postRepo.findById(post_id).orElseThrow(()->new ResourceNotFoundException("Post "," Id ", post_id));
		Set<Integer> reactedUsers = post.getReactUsers();
//		if(reactedUsers.contains(userId)) reactedUsers.remove(userId);
//		else 
			reactedUsers.add(userId);
		post.setLikes(reactedUsers.size());
		Post updatedPost = this.postRepo.save(post);
		return this.postToDto(updatedPost);
	}

	@Override
	public void deletePost(Integer id) {
		Post post = postRepo.findById(id).orElseThrow(()-> new ResourceNotFoundException("Post ", "Id ", id));
		postRepo.delete(post);

	}

	@Override
	public PostResponse getAllPosts(Integer pageNumber, Integer pageSize, String sortBy, String orderBy) {
		
		Sort sort = (orderBy.equalsIgnoreCase("asc"))? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
				
		Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);
		
		Page<Post> pages = this.postRepo.findAll(pageable);
		List<Post> posts= pages.getContent();
		
		List<PostDto> postDtos = posts.stream().map((post)->this.postToDto(post)).collect(Collectors.toList());
		
		for(PostDto dto : postDtos) {
			List<String> urlsList = imageUploader.getAllImagesByPost(dto.getImages());
			dto.setImages(urlsList);
		}
// 		
		PostResponse postResponse = new PostResponse();
		postResponse.setContent(postDtos);
		postResponse.setPageNumber(pages.getNumber());
		postResponse.setPageSize(pages.getSize());
		postResponse.setTotalPosts(pages.getTotalElements());
		postResponse.setTotalPages(pages.getTotalPages());
		postResponse.setLastPage(pages.isLast());
		
		return postResponse;
	}

	@Override
	public PostDto getPostById(Integer id) {
		Post post = this.postRepo.findById(id).orElseThrow(()->new ResourceNotFoundException("Post ","Id ", id));
		PostDto dto = this.postToDto(post);
		List<String> allImagesByPost = imageUploader.getAllImagesByPost(dto.getImages());
		dto.setImages(allImagesByPost);
		return dto;
	}

	@Override
	public List<PostDto> getPostsByCategory(Integer id) {
		Category category = this.categoryRepo.findById(id).orElseThrow(()-> new ResourceNotFoundException("Category ", "Id ", id));
		List<Post> posts = this.postRepo.findByCategory(category);
		List<PostDto> postDtos = posts.stream().map((post)->this.postToDto(post)).collect(Collectors.toList());
		for(PostDto dto : postDtos) {
			List<String> urlsList = imageUploader.getAllImagesByPost(dto.getImages());
			dto.setImages(urlsList);
		}
		return postDtos;
	}

	@Override
	public List<PostDto> getPostsByUser(Integer id) {
		User user = this.userRepo.findById(id).orElseThrow(()-> new ResourceNotFoundException("User ", "Id ", id));
		List<Post> posts = this.postRepo.findByUser(user);
		List<PostDto> postDtos = posts.stream().map((post)->this.postToDto(post)).collect(Collectors.toList());
		for(PostDto dto : postDtos) {
			List<String> urlsList = imageUploader.getAllImagesByPost(dto.getImages());
			dto.setImages(urlsList);
		}
		return postDtos;
	}

	@Override
	public List<PostDto> searchPosts(String keyword) {
		List<Post> posts =  this.postRepo.findByTitleContainingAllIgnoreCase(keyword);
		List<PostDto> postDtos = posts.stream().map((p)-> postToDto(p)).collect(Collectors.toList());
		for(PostDto dto : postDtos) {
			List<String> urlsList = imageUploader.getAllImagesByPost(dto.getImages());
			dto.setImages(urlsList);
		}
		return postDtos;
	}
	
	private Post dtoToPost(PostDto dto) {
		Post post = this.modelMapper.map(dto, Post.class);
		return post;
	}
	
	private PostDto postToDto(Post post) {
		PostDto dto = this.modelMapper.map(post, PostDto.class);
		
		String formattedDate ="";
		LocalDate date = post.getCreationDate();
		if(date!=null)
		formattedDate = date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM));
		dto.setCreationDate(formattedDate);
		
		LocalTime time = post.getCreationTime();
		if(time!=null)
		formattedDate = time.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM));
		dto.setCreatTime(formattedDate);
		
		LocalDateTime dateTime = post.getLastUpdatedDate();
		if(dateTime!=null)
		formattedDate = dateTime.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM));
		dto.setLastUpatedDateandTime(formattedDate);
		
		return dto;
	}

	@Override
	public PostDto create(String data, Integer user_id, Integer category_id, MultipartFile[] files) throws IOException {
		PostDto dto = this.objectMapper.readValue(data,PostDto.class);
		if(files!=null){
			List<String> imageList = Arrays.stream(files).map(file -> this.imageUploader.uploadImage("blog-images", file)).collect(Collectors.toList());
			dto.setImages(imageList);
	    } else dto.setImages(null);   
		PostDto saveDto = this.create(dto, user_id, category_id);
		return saveDto;
	}

	@Override
	public PostDto update(String dto, Integer post_id, MultipartFile file) {
		
		return null;
	}

}
