exports.seed = async function(knex) {
  // First, make sure we have lessons to version
  const lessons = await knex('lessons').select('id', 'lesson_number').orderBy('lesson_number');
  
  if (lessons.length === 0) {
    console.log('No lessons found. Please run lesson seeds first.');
    return;
  }

  // Delete existing content versions
  await knex('content_versions').del();
  
  // Create initial content versions for each lesson
  const contentVersions = [];
  
  for (const lesson of lessons.slice(0, 3)) { // Sample versions for first 3 lessons
    contentVersions.push({
      id: knex.raw('gen_random_uuid()'),
      lesson_id: lesson.id,
      version_number: 1,
      title: `Lesson ${lesson.lesson_number} - Initial Version`,
      description: 'Initial published version of the lesson content',
      content_structure: JSON.stringify({
        version: '1.0',
        lastModified: new Date().toISOString(),
        sections: {
          introduction: {
            type: 'text',
            content: `This is the initial content for lesson ${lesson.lesson_number}`
          },
          video: {
            type: 'video',
            url: `https://example.com/videos/lesson${lesson.lesson_number}.mp4`,
            thumbnail: `https://example.com/thumbnails/lesson${lesson.lesson_number}.jpg`,
            duration: 480
          },
          activities: {
            type: 'activities',
            items: [
              {
                id: `activity-${lesson.lesson_number}-1`,
                title: 'Discussion Activity',
                type: 'discussion',
                duration: 15
              }
            ]
          }
        },
        facilitatorNotes: {
          preparation: 'Prepare discussion questions and materials',
          timing: 'Allow flexibility for student responses',
          adaptations: 'Consider student age and classroom dynamics'
        }
      }),
      metadata: JSON.stringify({
        educationalLevel: 'grades-4-6',
        subject: 'social-emotional-learning',
        curriculum: 'heroes-in-waiting',
        antiABullyingFocus: true,
        coppaCompliant: true,
        accessibility: {
          screenReaderFriendly: true,
          visualAids: true,
          multipleFormats: true
        }
      }),
      created_by: knex.raw('(SELECT id FROM facilitators LIMIT 1)'), // Use first facilitator
      change_summary: 'Initial version created for lesson content management',
      status: 'published'
    });
  }
  
  // Insert content versions
  await knex('content_versions').insert(contentVersions);
  
  console.log(`Created ${contentVersions.length} content versions for lesson management`);
};