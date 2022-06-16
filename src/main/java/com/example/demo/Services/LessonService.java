package com.example.demo.Services;

import com.example.demo.Entities.Lesson;
import com.example.demo.Entities.User;
import com.example.demo.Repositories.LessonRepository;
import com.example.demo.Repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
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
    public void signToLesson(Long lessonId, User user){
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
        //temat powinien byc enumem, jak zdaze to poprawie
        Integer globalSum=0,sum1=0,sum2=0,sum3=0;
        List<Lesson> list1 = lessonRepository.getLessonByTopic("Topic1");
        List <Lesson> list2 = lessonRepository.getLessonByTopic("Topic2");
        List <Lesson> list3 = lessonRepository.getLessonByTopic("Topic3");
        for (Lesson l:list1
             ) {for(User user : l.getUsers()){
                 globalSum++;
            sum1++;
        }

        }
        for (Lesson l:list2
        ) {for(User user : l.getUsers()){
            sum2++;
            globalSum++;
        }

        }
        for (Lesson l:list3
        ) {for(User user : l.getUsers()){
            sum3++;
            globalSum++;
        }

        }
        map.put("Topic1",(float)sum1/(float)globalSum*100);
        map.put("Topic2",(float)sum2/(float)globalSum*100);
        map.put("Topic3",(float)sum3/(float)globalSum*100);
        return map;
    }
}
