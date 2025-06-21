exports.seed = async function(knex) {
  // Deletes ALL existing entries
  await knex('lessons').del();
  
  // Insert sample lessons
  await knex('lessons').insert([
    {
      id: knex.raw('gen_random_uuid()'),
      lesson_number: 1,
      title: 'What Makes a Hero?',
      description: 'Introduction to heroic qualities and how ordinary people can be heroes in their daily lives.',
      learning_objectives: 'Students will identify heroic qualities and understand that anyone can be a hero.',
      duration_minutes: 30,
      difficulty_level: 'beginner',
      content_structure: JSON.stringify({
        sections: [
          { type: 'video', title: 'Heroes Around Us', duration: 8 },
          { type: 'discussion', title: 'What Makes Someone a Hero?', duration: 10 },
          { type: 'activity', title: 'Hero Gallery Walk', duration: 12 }
        ]
      }),
      video_url: 'https://example.com/videos/lesson1.mp4',
      video_thumbnail: 'https://example.com/thumbnails/lesson1.jpg',
      video_duration_seconds: 480,
      downloadable_resources: JSON.stringify([
        'Hero pictures or cards',
        'Chart paper',
        'Markers',
        'Student worksheets'
      ]),
      activities: JSON.stringify([
        {
          id: 'hero-brainstorm',
          title: 'Hero Brainstorm',
          type: 'discussion',
          description: 'Students share examples of heroes from their lives',
          duration: 10
        },
        {
          id: 'hero-qualities',
          title: 'Identifying Hero Qualities',
          type: 'group-work',
          description: 'Small groups identify common qualities of heroes',
          duration: 15
        }
      ]),
      discussion_questions: JSON.stringify([
        'Who are some heroes in your life?',
        'What qualities do heroes have in common?',
        'How can you be a hero in your classroom?',
        'What is the difference between a superhero and a real-life hero?'
      ]),
      is_published: true,
      sort_order: 1
    },
    {
      id: knex.raw('gen_random_uuid()'),
      lesson_number: 2,
      title: 'Building Connections',
      description: 'Understanding the importance of friendship and positive relationships in creating a caring community.',
      learning_objectives: 'Students will learn how to build and maintain positive relationships with their peers.',
      duration_minutes: 35,
      difficulty_level: 'beginner',
      content_structure: JSON.stringify({
        sections: [
          { type: 'video', title: 'The Power of Friendship', duration: 7 },
          { type: 'activity', title: 'Friendship Web', duration: 15 },
          { type: 'discussion', title: 'How to Be a Good Friend', duration: 8 },
          { type: 'reflection', title: 'Friendship Goals', duration: 5 }
        ]
      }),
      video_url: 'https://example.com/videos/lesson2.mp4',
      video_thumbnail: 'https://example.com/thumbnails/lesson2.jpg',
      video_duration_seconds: 420,
      downloadable_resources: JSON.stringify([
        'Ball of yarn',
        'Friendship cards',
        'Reflection journals',
        'Colored pencils'
      ]),
      activities: JSON.stringify([
        {
          id: 'friendship-web',
          title: 'Friendship Web Activity',
          type: 'physical',
          description: 'Students create a web showing connections between classmates',
          duration: 15
        },
        {
          id: 'kindness-cards',
          title: 'Kindness Cards',
          type: 'creative',
          description: 'Students create cards with kind messages for classmates',
          duration: 12
        }
      ]),
      discussion_questions: JSON.stringify([
        'What makes someone a good friend?',
        'How do you feel when someone is kind to you?',
        'What can you do if someone feels left out?',
        'How do friendships make our classroom better?'
      ]),
      is_published: true,
      sort_order: 2
    },
    {
      id: knex.raw('gen_random_uuid()'),
      lesson_number: 3,
      title: 'Understanding Feelings',
      description: 'Developing emotional awareness and empathy for others through perspective-taking activities.',
      learning_objectives: 'Students will identify emotions and practice understanding how others feel.',
      duration_minutes: 40,
      difficulty_level: 'intermediate',
      content_structure: JSON.stringify({
        sections: [
          { type: 'video', title: 'Reading Emotions', duration: 6 },
          { type: 'activity', title: 'Emotion Charades', duration: 15 },
          { type: 'discussion', title: 'Perspective Taking', duration: 12 },
          { type: 'reflection', title: 'Empathy Journal', duration: 7 }
        ]
      }),
      video_url: 'https://example.com/videos/lesson3.mp4',
      video_thumbnail: 'https://example.com/thumbnails/lesson3.jpg',
      video_duration_seconds: 360,
      downloadable_resources: JSON.stringify([
        'Emotion cards',
        'Mirrors',
        'Scenario cards',
        'Journals'
      ]),
      activities: JSON.stringify([
        {
          id: 'emotion-identification',
          title: 'Emotion Detective',
          type: 'interactive',
          description: 'Students practice identifying emotions in facial expressions',
          scenarios: [
            'A classmate drops their lunch',
            'Someone gets a good grade on a test',
            'A student is sitting alone at recess',
            'A classmate gets a lower grade on a test'
          ]
        }
      ]),
      discussion_questions: JSON.stringify([
        'How do you know when someone is feeling sad?',
        'What clues help us understand how someone is feeling?',
        'How can we show that we care about others?',
        'When have you felt understood by someone else?'
      ]),
      is_published: true,
      sort_order: 3
    }
  ]);
};