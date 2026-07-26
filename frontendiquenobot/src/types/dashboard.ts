export interface DashboardOverview {
  conversationStats: ConversationStats
  leadStats: LeadStats
  contactStats: ContactStats
  productStats: ProductStats
}

export interface ConversationStats {
  totalConversations: number
  activeConversations: number
  openConversations: number
  closedConversations: number
  pendingConversations: number
  averageResponseTimeMinutes: number
  averageResolutionTimeHours: number
  conversationsToday: number
  conversationsThisWeek: number
  conversationsThisMonth: number
}

export interface LeadStats {
  totalLeads: number
  newLeads: number
  contactedLeads: number
  qualifiedLeads: number
  convertedLeads: number
  lostLeads: number
  conversionRate: number
  averageLeadScore: number
  leadsToday: number
  leadsThisWeek: number
  leadsThisMonth: number
  unassignedLeads: number
  highScoreLeads: number
}

export interface ContactStats {
  totalContacts: number
  activeContacts: number
  blockedContacts: number
  subscribedContacts: number
  contactsToday: number
  contactsThisWeek: number
  contactsThisMonth: number
  contactsWithConversations: number
}

export interface ProductStats {
  totalProducts: number
  activeProducts: number
  outOfStockProducts: number
  lowStockProducts: number
  featuredProducts: number
  totalCategories: number
}

export interface UserActivity {
  userId: string
  userName: string
  userEmail: string
  assignedConversations: number
  closedConversations: number
  assignedLeads: number
  convertedLeads: number
  sentMessages: number
}
