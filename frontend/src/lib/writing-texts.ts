// Curated writing-practice content. Each lesson provides a set of texts the learner
// copies stroke-by-stroke, together with the pinyin reading and the English translation.
//
// Characters are kept within the common HSK 1-3 range so that `hanzi-writer-data`
// reliably ships stroke data for every glyph used here.

export interface WritingText {
  /** The Chinese text to write (single character, word, phrase, or sentence). */
  hanzi: string;
  /**
   * Pinyin reading with tone marks, syllables separated by spaces. Optional:
   * curated lessons provide it; long-form corpus texts (e.g. a novel chapter)
   * are written from the characters alone.
   */
  pinyin?: string;
  /** English translation shown as the prompt. Optional for corpus texts. */
  english?: string;
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
  {
    id: "three-kingdoms",
    title: "Romance of the Three Kingdoms",
    description: "Lines from the opening of 《三国演义》 and its prologue poem.",
    level: 3,
    texts: [
      {
        hanzi: "话说天下大势，分久必合，合久必分。",
        pinyin: "huà shuō tiān xià dà shì, fēn jiǔ bì hé, hé jiǔ bì fēn.",
        english: "The empire, long divided, must unite; long united, must divide.",
      },
      {
        hanzi: "滚滚长江东逝水，浪花淘尽英雄。",
        pinyin: "gǔn gǔn cháng jiāng dōng shì shuǐ, làng huā táo jìn yīng xióng.",
        english: "The mighty Yangtze flows ever eastward; its waves wash away all heroes.",
      },
      {
        hanzi: "是非成败转头空。",
        pinyin: "shì fēi chéng bài zhuǎn tóu kōng.",
        english: "Right and wrong, triumph and defeat — all turn to nothing in a moment.",
      },
      {
        hanzi: "青山依旧在，几度夕阳红。",
        pinyin: "qīng shān yī jiù zài, jǐ dù xī yáng hóng.",
        english: "The green hills remain; how often has the setting sun glowed red.",
      },
    ],
  },
  {
    id: "sayings-of-the-sages",
    title: "Sayings of the Sages",
    description: "Famous lines from the Analects 《论语》 and the Tao Te Ching 《道德经》.",
    level: 3,
    texts: [
      {
        hanzi: "学而时习之，不亦说乎？",
        pinyin: "xué ér shí xí zhī, bù yì yuè hū?",
        english: "To learn and practise in due time — is that not a pleasure?",
      },
      {
        hanzi: "有朋自远方来，不亦乐乎？",
        pinyin: "yǒu péng zì yuǎn fāng lái, bù yì lè hū?",
        english: "To have friends come from afar — is that not a joy?",
      },
      {
        hanzi: "三人行，必有我师焉。",
        pinyin: "sān rén xíng, bì yǒu wǒ shī yān.",
        english: "Among any three people walking, one can surely be my teacher.",
      },
      {
        hanzi: "道可道，非常道。",
        pinyin: "dào kě dào, fēi cháng dào.",
        english: "The Tao that can be spoken is not the eternal Tao.",
      },
      {
        hanzi: "千里之行，始于足下。",
        pinyin: "qiān lǐ zhī xíng, shǐ yú zú xià.",
        english: "A journey of a thousand miles begins with a single step.",
      },
    ],
  },
  {
    id: "tang-poems",
    title: "Tang Poetry — Li Bai",
    description: "“Quiet Night Thoughts” 《静夜思》, one of the best-loved Tang poems.",
    level: 3,
    texts: [
      { hanzi: "床前明月光，", pinyin: "chuáng qián míng yuè guāng,", english: "Before my bed, the bright moonlight," },
      { hanzi: "疑是地上霜。", pinyin: "yí shì dì shàng shuāng.", english: "I wonder if it is frost upon the ground." },
      { hanzi: "举头望明月，", pinyin: "jǔ tóu wàng míng yuè,", english: "I raise my head and gaze at the bright moon," },
      { hanzi: "低头思故乡。", pinyin: "dī tóu sī gù xiāng.", english: "I lower my head and think of my homeland." },
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
