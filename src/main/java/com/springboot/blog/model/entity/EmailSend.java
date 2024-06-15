package com.springboot.blog.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(
        name = "email_sends"
)
public class EmailSend {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "email_send_id")
    private Long id;

    @Column(nullable = false)
    private String content;

    @Column(nullable = false)
    private String sendDate;

    @ManyToOne
    @JoinColumn(name = "template_id", referencedColumnName = "template_id")
    private EmailTemplate emailTemplate;

}
