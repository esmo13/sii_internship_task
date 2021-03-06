package com.example.demo.Services;

import com.example.demo.Entities.Lesson;
import com.example.demo.Entities.User;
import com.example.demo.Repositories.LessonRepository;
import com.example.demo.Repositories.UserRepository;
import org.apache.tomcat.jni.Local;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class LessonService {
    private final LessonRepository lessonRepository;
    private final UserService userService;
    @Autowired
    LessonService(LessonRepository lessonRepository,UserService userService){
        this.lessonRepository=lessonRepository;
        this.userService = userService;

    }
    public List<Lesson> getLessons(){
        return lessonRepository.findAll();
    }
    public List<Lesson> getReservedLessons(String username){
    return lessonRepository.findAllByUsersContaining(userService.getUserByName(username));
    }
    @Transactional
    public void signToLesson(Long lessonId, User user) throws IOException {
        User user_ ;
        if(userService.existsByName(user.getLogin())) {
             user_ = userService.getUserByName(user.getLogin());
             if(!user_.getEmail().equals(user.getEmail()) ){
                 throw new IllegalStateException("user with such login already exists");
             }
        }
        else{
             user_ = userService.addUser(user);
        }

       Lesson lesson = lessonRepository.findById(lessonId).orElseThrow(()-> new IllegalStateException("lesson not found"));
        Set<User> set = lesson.getUsers();
        List<Lesson> list = lessonRepository.findAllByDateTime(lesson.getDateTime());
        for (Lesson lesson_:list
             ) {
            if(!lesson_.equals(lesson) && lesson_.getUsers().contains(user_))
                throw new IllegalStateException("You are already signed for a lesson at that time");

        }
        if(set.size()>=5){
            throw new IllegalStateException("lesson is full");
        }
        else if (set.contains(user_)){
            throw new IllegalStateException("You are already signed for this lesson");
        }

        else{
            set.add(user_);
            lesson.setUsers(set);
            LocalDateTime sent = LocalDateTime.now();
            String mail = "Sent at:"+sent +"TO:"+user_.getEmail()+"You were successfully signed to lesson" + lesson.toString()+"\n";


            BufferedWriter writer = new BufferedWriter(new FileWriter("powiadomienia.txt",true));
            writer.write(mail);
            writer.close();
        }

    }
    @Transactional
    public void unsignFromLesson(Long lessonId, User user){
        User user_ = userService.getUserByNameAndEmail(user.getLogin(),user.getEmail()) ;
        if (user_ != null) {
            Lesson lesson = lessonRepository.findById(lessonId).orElseThrow(()-> new IllegalStateException("lesson not found"));
            Set<User> set = lesson.getUsers();
            if(set.contains(user_)){
                set.remove(user_);
                lesson.setUsers(set);
            }
            else{
                throw new IllegalStateException("You are not signed for this lesson");
            }
        }
        else
            throw new IllegalStateException("user does not exist");

    }

    public Map<String, Float> getLessonPopulatiry() {
        List<Lesson> lessons=lessonRepository.findAll();
        Map<String,Float> map = new ManagedMap<>();
        for (Lesson l:lessons
             ) {
            int sum=0;
            for(User user : l.getUsers()){
                sum++;
            }
           map.put(l.toString(),(float)sum/ userService.countUsers() * 100);
        }
        return map;
    }

    public Map<String, Float> getTopicPopularity() {
        Map<String,Float> map = new ManagedMap<>();
        List<String> topics = lessonRepository.getDistinctTopics();
        Integer globalSum=0;
        List<Lesson> list;
        for (String topic:topics
             ) {
                map.put(topic,(float)0);
                list=lessonRepository.getLessonByTopic(topic);
            for (Lesson l:list
                 ) {
                for (User u:l.getUsers()
                     ) {
                    globalSum++;
                    map.put(topic,map.get(topic)+1);
                }
            }


        }
        for (String topic:topics
             ) {
            map.put(topic,map.get(topic)/(float)globalSum*100);
        }

        return map;
    }
}
