// Curated writing-practice content. Each lesson provides a set of texts the learner
// copies stroke-by-stroke, together with the pinyin reading and the English translation.
//
// Characters are kept within the common HSK 1-3 range so that `hanzi-writer-data`
// reliably ships stroke data for every glyph used here.

export interface WritingText {
  /** The Chinese text to write (single character, word, or short phrase). */
  hanzi: string;
  /** Pinyin reading with tone marks, syllables separated by spaces. */
  pinyin: string;
  /** English translation shown as the prompt. */
  english: string;
}

export interface WritingLesson {
  id: string;
  /** English title shown on the lesson card. */
  title: string;
  /** One-line English description of the lesson. */
  description: string;
  /** Approximate HSK level, used purely as a difficulty hint in the UI. */
  level: 1 | 2 | 3;
  texts: WritingText[];
}

export const WRITING_LESSONS: WritingLesson[] = [
  {
    id: "greetings",
    title: "Greetings & Politeness",
    description: "The phrases you use first in any conversation.",
    level: 1,
    texts: [
      { hanzi: "你好", pinyin: "nǐ hǎo", english: "hello" },
      { hanzi: "谢谢", pinyin: "xiè xie", english: "thank you" },
      { hanzi: "不客气", pinyin: "bú kè qi", english: "you're welcome" },
      { hanzi: "对不起", pinyin: "duì bu qǐ", english: "sorry" },
      { hanzi: "没关系", pinyin: "méi guān xi", english: "it doesn't matter" },
      { hanzi: "再见", pinyin: "zài jiàn", english: "goodbye" },
    ],
  },
  {
    id: "numbers",
    title: "Numbers 1–10",
    description: "Foundational characters with simple stroke orders.",
    level: 1,
    texts: [
      { hanzi: "一", pinyin: "yī", english: "one" },
      { hanzi: "二", pinyin: "èr", english: "two" },
      { hanzi: "三", pinyin: "sān", english: "three" },
      { hanzi: "四", pinyin: "sì", english: "four" },
      { hanzi: "五", pinyin: "wǔ", english: "five" },
      { hanzi: "六", pinyin: "liù", english: "six" },
      { hanzi: "七", pinyin: "qī", english: "seven" },
      { hanzi: "八", pinyin: "bā", english: "eight" },
      { hanzi: "九", pinyin: "jiǔ", english: "nine" },
      { hanzi: "十", pinyin: "shí", english: "ten" },
    ],
  },
  {
    id: "people-family",
    title: "People & Family",
    description: "Words for the people closest to you.",
    level: 1,
    texts: [
      { hanzi: "我", pinyin: "wǒ", english: "I, me" },
      { hanzi: "你", pinyin: "nǐ", english: "you" },
      { hanzi: "他", pinyin: "tā", english: "he, him" },
      { hanzi: "她", pinyin: "tā", english: "she, her" },
      { hanzi: "妈妈", pinyin: "mā ma", english: "mom" },
      { hanzi: "爸爸", pinyin: "bà ba", english: "dad" },
      { hanzi: "朋友", pinyin: "péng you", english: "friend" },
      { hanzi: "老师", pinyin: "lǎo shī", english: "teacher" },
    ],
  },
  {
    id: "everyday-words",
    title: "Everyday Words",
    description: "High-frequency nouns you meet every day.",
    level: 2,
    texts: [
      { hanzi: "中国", pinyin: "zhōng guó", english: "China" },
      { hanzi: "学生", pinyin: "xué sheng", english: "student" },
      { hanzi: "学校", pinyin: "xué xiào", english: "school" },
      { hanzi: "时间", pinyin: "shí jiān", english: "time" },
      { hanzi: "今天", pinyin: "jīn tiān", english: "today" },
      { hanzi: "明天", pinyin: "míng tiān", english: "tomorrow" },
      { hanzi: "工作", pinyin: "gōng zuò", english: "work, job" },
      { hanzi: "电话", pinyin: "diàn huà", english: "telephone" },
    ],
  },
  {
    id: "useful-phrases",
    title: "Useful Phrases",
    description: "Short sentences for real conversations.",
    level: 3,
    texts: [
      { hanzi: "我爱你", pinyin: "wǒ ài nǐ", english: "I love you" },
      { hanzi: "我不知道", pinyin: "wǒ bù zhī dào", english: "I don't know" },
      { hanzi: "你叫什么名字", pinyin: "nǐ jiào shén me míng zi", english: "what is your name?" },
      { hanzi: "我很高兴", pinyin: "wǒ hěn gāo xìng", english: "I'm very happy" },
      { hanzi: "多少钱", pinyin: "duō shao qián", english: "how much money?" },
      { hanzi: "我喜欢学中文", pinyin: "wǒ xǐ huan xué zhōng wén", english: "I like learning Chinese" },
    ],
  },
  {
    id: "short-sentences",
    title: "Short Sentences",
    description: "Complete sentences with punctuation — write them out in full.",
    level: 2,
    texts: [
      { hanzi: "我是学生。", pinyin: "wǒ shì xué sheng.", english: "I am a student." },
      { hanzi: "他是我的朋友。", pinyin: "tā shì wǒ de péng you.", english: "He is my friend." },
      { hanzi: "今天天气很好。", pinyin: "jīn tiān tiān qì hěn hǎo.", english: "The weather is nice today." },
      { hanzi: "这是我的家。", pinyin: "zhè shì wǒ de jiā.", english: "This is my home." },
      { hanzi: "我喜欢喝茶。", pinyin: "wǒ xǐ huan hē chá.", english: "I like drinking tea." },
      { hanzi: "你想吃什么？", pinyin: "nǐ xiǎng chī shén me?", english: "What do you want to eat?" },
    ],
  },
  {
    id: "daily-sentences",
    title: "Everyday Sentences",
    description: "Longer sentences you might say on an ordinary day.",
    level: 3,
    texts: [
      { hanzi: "我每天早上喝咖啡。", pinyin: "wǒ měi tiān zǎo shang hē kā fēi.", english: "I drink coffee every morning." },
      { hanzi: "周末我喜欢看电影。", pinyin: "zhōu mò wǒ xǐ huan kàn diàn yǐng.", english: "On weekends I like watching movies." },
      { hanzi: "我们一起去吃饭吧。", pinyin: "wǒ men yì qǐ qù chī fàn ba.", english: "Let's go eat together." },
      { hanzi: "这本书很有意思。", pinyin: "zhè běn shū hěn yǒu yì si.", english: "This book is very interesting." },
      { hanzi: "我学习中文已经一年了。", pinyin: "wǒ xué xí zhōng wén yǐ jīng yì nián le.", english: "I have been studying Chinese for a year." },
    ],
  },
];

export function findLesson(id: string | null | undefined): WritingLesson | undefined {
  if (!id) return undefined;
  return WRITING_LESSONS.find((lesson) => lesson.id === id);
}

/** True for CJK ideographs — the only glyphs `hanzi-writer` can render strokes for. */
export function isWritable(char: string): boolean {
  return /[一-鿿]/.test(char);
}

/** The drawable characters of a text, with punctuation and spaces stripped out. */
export function writableChars(hanzi: string): string[] {
  return Array.from(hanzi).filter(isWritable);
}

/** Total number of glyphs the learner writes to finish a lesson (punctuation excluded). */
export function countCharacters(lesson: WritingLesson): number {
  return lesson.texts.reduce((sum, text) => sum + writableChars(text.hanzi).length, 0);
}
